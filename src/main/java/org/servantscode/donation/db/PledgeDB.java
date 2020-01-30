package org.servantscode.donation.db;

import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Pledge;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PledgeDB extends EasyDB<Pledge> {

    private static final Map<String, String> FIELD_MAP = new HashMap<>();

    static {
        FIELD_MAP.put("familyName", "family_name");
        FIELD_MAP.put("fundName", "fund_name");
        FIELD_MAP.put("pledgeStart", "pledge_start");
        FIELD_MAP.put("pledgeEnd", "pledge_end");
        FIELD_MAP.put("pledgeType", "pledge_type");
        FIELD_MAP.put("pledgeFrequency", "frequency");
        FIELD_MAP.put("annualPledgeAmount", "total_pledge");
        FIELD_MAP.put("completedAmount", "total_donations");
        FIELD_MAP.put("completionScore", "completion_score");
        FIELD_MAP.put("pledgeStatus", "pledge_status");
    }

    public PledgeDB() {
        super(Pledge.class, "familyName", FIELD_MAP);
    }

    //Query setup
    private QueryBuilder select(QueryBuilder selection) {
        //If you are reading this, I'm sorry.
        //Anyone who comes up with a better way earns a coffee. [GRL]

        //3 level subquery necessary to
        //1) Compute collected_pct and time_pct
        //2) Compute completion_score and pledge_status
        //3) Make pledge status and completion_score available for query

        return selection
            .from(select("*")
                 .select("collected_pct - time_pct AS completion_score")
                 .select("CASE WHEN total_donations >= total_pledge THEN 'COMPLETED' " +
                              "WHEN collected_pct - time_pct > -0.04 THEN 'CURRENT' " +
                              "WHEN collected_pct - time_pct > -0.082 THEN 'SLIGHTLY_BEHIND' " +
                              "WHEN total_donations > 0 THEN 'BEHIND' " +
                              "ELSE 'NOT_STARTED' END AS pledge_status")
                 .from(select("p.*", "f.name AS fund_name", "fam.surname AS family_name", "d.total_donations")
                      .select("d.total_donations/p.total_pledge AS collected_pct")
                       //Ths can be better in postgres 12 when subtracting two dates gives you a number of days...
                       //Alas... still on 11.5 in production
                       //.select("(current_date - pledge_start)*1.0/(pledge_end - pledge_start) AS time_pct")
                      .select("(EXTRACT(epoch from current_date) - EXTRACT(EPOCH FROM pledge_start))*1.0/(EXTRACT(EPOCH FROM pledge_end) - EXTRACT(EPOCH FROM pledge_start)) AS time_pct")
                      .from("pledges p")
                      .leftJoin("funds f ON f.id=p.fund_id")
                      .leftJoin("families fam ON fam.id=p.family_id")
                      .leftJoinLateral(select("pledge_id", "SUM(amount) AS total_donations").from("donations")
                                      .groupBy("pledge_id"),
                              "d", "pledge_id=p.id")
//                      .leftJoinLateral(select("family_id", "fund_id", "SUM(amount) AS total_donations").from("donations")
//                                     .where("date >= p.pledge_start AND date <= p.pledge_end").inOrg()
//                                     .groupBy("family_id", "fund_id"),
//                             "d", "p.family_id=d.family_id AND p.fund_id=d.fund_id")
                      .inOrg("p.org_id"),
                "a"),
            "final");
    }

    //Interface
    public int getActivePledgeCount(String search) {
        QueryBuilder query = select(count()).search(searchParser.parse(search));
        return getCount(query);
    }

    public List<Pledge> getActivePledges(int start, int count, String sortField, String search) {
        QueryBuilder query = select(selectAll()).search(searchParser.parse(search))
                .page(sortField, start, count);
        return get(query);
    }

    public Pledge getActivePledge(int familyId, int fundId) {
        QueryBuilder query = select(selectAll()).with("family_id", familyId).with("fund_id", fundId)
                .where("pledge_start <= NOW() AND pledge_end >= NOW()");
        return getOne(query);
    }

    public Pledge getRelaventPledge(int familyId, int fundId, LocalDate donationDate) {
        QueryBuilder query = select(selectAll()).with("family_id", familyId).with("fund_id", fundId)
                .where("pledge_start <= ? AND pledge_end >= ?", donationDate, donationDate);
        return getOne(query);
    }

    public List<Pledge> getActiveFamilyPledges(int familyId) {
        QueryBuilder query = select(selectAll()).with("family_id", familyId)
                .where("pledge_start <= NOW() AND pledge_end >= NOW()");
        return get(query);
    }

    public List<Pledge> getFamilyPledges(int familyId) {
        QueryBuilder query = select(selectAll()).with("family_id", familyId)
                .sort("pledge_end DESC");
        return get(query);
    }

    public StreamingOutput getReportReader(String search, final List<String> fields) {
        QueryBuilder query = select(selectAll()).search(searchParser.parse(search));
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

    public Pledge createPledge(Pledge pledge) {
        InsertBuilder cmd = insertInto("pledges")
                .value("family_id", pledge.getFamilyId())
                .value("fund_id", pledge.getFundId())
                .value("pledge_type", pledge.getPledgeType())
                .value("pledge_date", pledge.getPledgeDate())
                .value("pledge_start", pledge.getPledgeStart())
                .value("pledge_end", pledge.getPledgeEnd())
                .value("frequency", pledge.getPledgeFrequency())
                .value("pledge_increment", pledge.getPledgeAmount())
                .value("total_pledge", pledge.getAnnualPledgeAmount())
                .value("org_id", OrganizationContext.orgId());

        pledge.setId(createAndReturnKey(cmd));
        return pledge;
    }

    public boolean updatePledge(Pledge pledge) {
        UpdateBuilder cmd = update("pledges")
                .value("family_id", pledge.getFamilyId())
                .value("fund_id", pledge.getFundId())
                .value("pledge_type", pledge.getPledgeType())
                .value("pledge_date", pledge.getPledgeDate())
                .value("pledge_start", pledge.getPledgeStart())
                .value("pledge_end", pledge.getPledgeEnd())
                .value("frequency", pledge.getPledgeFrequency())
                .value("pledge_increment", pledge.getPledgeAmount())
                .value("total_pledge", pledge.getAnnualPledgeAmount())
                .withId(pledge.getId()).inOrg();

        return update(cmd);
    }

    public boolean deletePledge(int pledgeId) {
        return delete(deleteFrom("pledges").withId(pledgeId).inOrg());
    }

    // ----- Private -----
    @Override
    protected Pledge processRow(ResultSet rs) throws SQLException {
        Pledge pledge = new Pledge();
        pledge.setId(rs.getInt("id"));
        pledge.setFamilyId(rs.getInt("family_id"));
        pledge.setFamilyName(rs.getString("family_name"));
        pledge.setFundId(rs.getInt("fund_id"));
        pledge.setFundName(rs.getString("fund_name"));
        pledge.setPledgeType(rs.getString("pledge_type"));
        pledge.setPledgeDate(convert(rs.getDate("pledge_date")));
        pledge.setPledgeStart(convert(rs.getDate("pledge_start")));
        pledge.setPledgeEnd(convert(rs.getDate("pledge_end")));
        pledge.setPledgeFrequency(rs.getString("frequency"));
        pledge.setPledgeAmount(rs.getFloat("pledge_increment"));
        pledge.setAnnualPledgeAmount(rs.getFloat("total_pledge"));
        pledge.setCollectedAmount(rs.getFloat("total_donations"));
        pledge.setCollectedPct(rs.getFloat("collected_pct"));
        pledge.setTimePct(rs.getFloat("time_pct"));
        pledge.setCompletionScore(rs.getFloat("completion_score"));
        pledge.setPledgeStatus(Pledge.PledgeStatus.valueOf(rs.getString("pledge_status")));
        return pledge;
    }
}
