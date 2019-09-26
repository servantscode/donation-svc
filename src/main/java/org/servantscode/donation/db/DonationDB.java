package org.servantscode.donation.db;

import org.servantscode.commons.StringUtils;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Donation;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class DonationDB extends EasyDB<Donation> {

    private static final HashMap<String, String> FIELD_MAP = new HashMap<>(8);

    static {
        FIELD_MAP.put("fundName", "f.name");
        FIELD_MAP.put("donationDate", "date");
        FIELD_MAP.put("donationType", "type");
    }

    public DonationDB() {
        super(Donation.class, "fundName", FIELD_MAP);
    }


    public int getDonationCount(String search) {
        QueryBuilder query = count().from("donations d","funds f").where("d.fund_id=f.id")
                .search(searchParser.parse(search)).inOrg("d.org_id");
        return getCount(query);
    }

    public float getDonationTotal(String search) {
        QueryBuilder query = select("sum(amount) AS total").from("donations d","funds f").where("d.fund_id=f.id")
                .search(searchParser.parse(search)).inOrg("d.org_id");
        try(Connection conn = getConnection();
            PreparedStatement stmt = query.prepareStatement(conn);
            ResultSet rs = stmt.executeQuery())
        {
            if(rs.next())
                return rs.getFloat(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve total donation value for search.", e);
        }
        return 0f;
    }

    public List<Donation> getDonations(int start, int count, String sortField, String search) {
        QueryBuilder query = baseQuery().search(searchParser.parse(search))
                .sort(sortField).limit(count).offset(start);
        return get(query);
    }

    private QueryBuilder baseQuery() {
        return select("d.*","f.name AS fund_name", "fam.surname AS family_name").from("donations d")
                .leftJoin("funds f on d.fund_id=f.id")
                .leftJoin("families fam on d.family_id=fam.id")
                .inOrg("d.org_id");
    }

    public int getFamilyDonationCount(int familyId, String search) {
        QueryBuilder query = count().from("donations d","funds f").where("d.fund_id=f.id")
                .where("family_id=?", familyId).search(searchParser.parse(search)).inOrg("d.org_id");
        return getCount(query);
    }

    public float getFamilyDonationTotal(int familyId, String search) {
        QueryBuilder query = select("sum(amount) AS total").from("donations d","funds f").where("d.fund_id=f.id")
                .with("family_id", familyId).search(searchParser.parse(search)).inOrg("d.org_id");
        try(Connection conn = getConnection();
            PreparedStatement stmt = query.prepareStatement(conn);
            ResultSet rs = stmt.executeQuery())
        {
            if(rs.next())
                return rs.getFloat(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve total donation value for search.", e);
        }
        return 0f;
    }

    public List<Donation> getFamilyDonations(int familyId, int start, int count, String sortField, String search) {
        QueryBuilder query = baseQuery().where("family_id=?", familyId).search(searchParser.parse(search))
                .sort(sortField).limit(count).offset(start);
        return get(query);
    }

    public Donation getLastDonation(int familyId, int fundId) {
        QueryBuilder query = baseQuery().where("family_id=?", familyId).where("fund_id=?", fundId).sort("date DESC").limit(1);
        return getOne(query);
    }

    public StreamingOutput getReportReader(String search, final List<String> fields) {
        QueryBuilder query = baseQuery().search(searchParser.parse(search));
        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try ( Connection conn = getConnection();
                      PreparedStatement stmt = query.prepareStatement(conn);
                      ResultSet rs = stmt.executeQuery()) {

                    writeCsv(output, rs);
                } catch (SQLException | IOException e) {
                    throw new RuntimeException("Could not retrieve donation report for search '" + search + "'", e);
                }
            }
        };
    }

    public Donation createDonation(Donation donation) {
        InsertBuilder cmd = insertInto("donations")
                .value("family_id", donation.getFamilyId())
                .value("fund_id", donation.getFundId())
                .value("amount", donation.getAmount())
                .value("date", convert(donation.getDonationDate()))
                .value("type", stringify(donation.getDonationType()))
                .value("check_number", donation.getCheckNumber())
                .value("transaction_id", donation.getTransactionId())
                .value("batch_number", donation.getBatchNumber())
                .value("notes", donation.getNotes())
                .value("org_id", OrganizationContext.orgId());
        donation.setId(createAndReturnKey(cmd));
        return donation;
    }

    public boolean updateDonation(Donation donation) {
        UpdateBuilder cmd = update("donations")
                .value("family_id", donation.getFamilyId())
                .value("fund_id", donation.getFundId())
                .value("amount", donation.getAmount())
                .value("date", convert(donation.getDonationDate()))
                .value("type", stringify(donation.getDonationType()))
                .value("check_number", donation.getCheckNumber())
                .value("transaction_id", donation.getTransactionId())
                .value("batch_number", donation.getBatchNumber())
                .value("notes", donation.getNotes())
                .withId(donation.getId()).inOrg();
        return update(cmd);
    }

    public boolean deleteDonation(long id) {
        return delete(deleteFrom("donations").withId(id).inOrg());
    }

    // ----- Private -----
    @Override
    protected Donation processRow(ResultSet rs) throws SQLException {
        Donation donation = new Donation();
        donation.setId(rs.getLong("id"));
        donation.setFamilyId(rs.getInt("family_id"));
        donation.setFamilyName(rs.getString("family_name"));
        donation.setFundId(rs.getInt("fund_id"));
        donation.setFundName(rs.getString("fund_name"));
        donation.setAmount(rs.getFloat("amount"));
        donation.setDonationDate(convert(rs.getDate("date")));
        donation.setDonationType(rs.getString("type"));
        donation.setCheckNumber(rs.getInt("check_number"));
        donation.setTransactionId(rs.getLong("transaction_id"));
        return donation;
    }
}
