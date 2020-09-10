package org.servantscode.donation.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.FieldTransformer;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.donation.FamilyContributions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class FamilyContributionDB extends EasyDB<FamilyContributions> {
    private static final Logger LOG = LogManager.getLogger(FamilyContributionDB.class);

    private static final FieldTransformer FIELD_MAP = new FieldTransformer();
    static {
        FIELD_MAP.put("familyName", "surname");
        FIELD_MAP.put("totalDonationValue", "total_amount");
        FIELD_MAP.put("totalDonations", "donation_count");
    }

    public FamilyContributionDB() {
        super(FamilyContributions.class, "surname", FIELD_MAP);
    }

    private QueryBuilder data() {
        return select("f.*", "h.name AS head_name", "s.name AS spouse_name", "COALESCE(SUM(d.amount),0) AS total_amount", "COUNT(d.amount) AS donation_count");
    }

    private QueryBuilder select(QueryBuilder fields, LocalDate startDate, LocalDate endDate) {
        return fields.from(data().from("families f")
                            .leftJoin("people h ON h.family_id = f.id AND h.head_of_house = true")
                            .leftJoin("relationships r ON h.id=r.subject_id AND r.relationship = 'SPOUSE'")
                            .leftJoin("people s ON s.id = r.other_id")
                            .leftJoin("donations d ON f.id=d.family_id AND d.date>=? and d.date<=?", startDate, endDate)
                            .inOrg("f.org_id")
                            .groupBy("f.id", "h.name", "s.name"),
                "query");
    }


    public int getFamilyTotalDonationCount(LocalDate startDate, LocalDate endDate, String search) {
        return getCount(select(count(), startDate, endDate).search(searchParser.parse(search)));
    }

    public List<FamilyContributions> getFamilyTotalDonations(LocalDate startDate, LocalDate endDate,
                                                             int start, int count, String sort, String search) {
        return get(select(selectAll(), startDate, endDate)
                    .search(searchParser.parse(search))
                    .page(sort, start, count));
    }

    public StreamingOutput getReportReader(LocalDate startDate, LocalDate endDate, String search, final List<String> fields) {
        QueryBuilder query = select(selectAll(), startDate, endDate).search(searchParser.parse(search));
        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws WebApplicationException {
                try (Connection conn = getConnection();
                     PreparedStatement stmt = query.prepareStatement(conn);
                     ResultSet rs = stmt.executeQuery()) {

                    writeCsv(output, rs);
                } catch (SQLException | IOException e) {
                    throw new RuntimeException("Could not retrieve donation report for search '" + search + "'", e);
                }
            }
        };
    }

    // ----- Protected -----
    @Override
    protected FamilyContributions processRow(ResultSet rs) throws SQLException {
        FamilyContributions fd = new FamilyContributions();
        fd.setFamilyId(rs.getInt("id"));
        fd.setFamilyName(rs.getString("surname"));
        fd.setHeadName(rs.getString("head_name"));
        fd.setSpouseName(rs.getString("spouse_name"));
        fd.setTotalDonations(rs.getInt("donation_count"));
        fd.setTotalDonationValue(rs.getInt("total_amount"));
        return fd;
    }

}
