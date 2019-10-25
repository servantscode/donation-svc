package org.servantscode.donation.db;

import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Pledge;

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
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;

public class PledgeDB extends EasyDB<Pledge> {

    private static final Map<String, String> FIELD_MAP = new HashMap<>();

    static {
        FIELD_MAP.put("familyName", "fam.surname");
        FIELD_MAP.put("fundName", "f.name");
        FIELD_MAP.put("pledgeStart", "pledge_start");
        FIELD_MAP.put("pledgeEnd", "pledge_end");
        FIELD_MAP.put("pledgeType", "pledge_type");
        FIELD_MAP.put("pledgeFrequency", "frequency");
        FIELD_MAP.put("annualPledgeAmount", "total_pledge");
        FIELD_MAP.put("completedAmount", "total_donations");
        FIELD_MAP.put("copmletionScore", "on_track_score");
    }

    public PledgeDB() {
        super(Pledge.class, "family_name", FIELD_MAP);
    }

    //Query setup
    private QueryBuilder all() {
        return select("p.*", "f.name AS fund_name", "fam.surname AS family_name", "d.total_donations", "d.total_donations/p.total_pledge AS collected_pct");
    }

    private QueryBuilder select(QueryBuilder selection) {
        return selection.from("pledges p")
                .leftJoin("funds f ON f.id=p.fund_id")
                .leftJoin("families fam ON fam.id=p.family_id")
                .leftJoinLateral(select("family_id", "fund_id", "SUM(amount) AS total_donations").from("donations")
                                .where("date >= p.pledge_start AND date <= p.pledge_end").inOrg()
                                .groupBy("family_id", "fund_id"),
                        "d", "p.family_id=d.family_id AND p.fund_id=d.fund_id")
                .inOrg("p.org_id");
    }

    //Interface
    public int getActivePledgeCount(String search) {
        QueryBuilder query = select(count()).search(searchParser.parse(search));
        return getCount(query);
    }

    public List<Pledge> getActivePledges(int start, int count, String sortField, String search) {
        QueryBuilder query = select(all()).search(searchParser.parse(search));
        query.page(sortField, start, count);
        return get(query);
    }

    public Pledge getActivePledge(int familyId, int fundId) {
        QueryBuilder query = select(all()).with("family_id", familyId).with("p.fund_id", fundId)
                .where("pledge_start <= NOW() AND pledge_end >= NOW()");
        return getOne(query);
    }

    public List<Pledge> getActiveFamilyPledges(int familyId) {
        QueryBuilder query = select(all()).with("p.family_id", familyId)
                .where("pledge_start <= NOW() AND pledge_end >= NOW()");
        return get(query);
    }

    public List<Pledge> getFamilyPledges(int familyId) {
        QueryBuilder query = select(all()).with("p.family_id", familyId)
                .sort("pledge_end DESC");
        return get(query);
    }

    public StreamingOutput getReportReader(String search, final List<String> fields) {
        QueryBuilder query = select(all()).search(searchParser.parse(search));
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
        long daysInPledge = DAYS.between(pledge.getPledgeStart(), pledge.getPledgeEnd());
        long daysSinceStart = DAYS.between(pledge.getPledgeStart(), LocalDate.now());
        pledge.setTimePct((daysSinceStart*1.0f)/daysInPledge);
        pledge.setCompletionScore(pledge.getCollectedPct()-pledge.getTimePct());
        pledge.setPledgeStatus(determinePledgeStatus(pledge));
        return pledge;
    }

    private Pledge.PledgeStatus determinePledgeStatus(Pledge p) {
        if(p.getCollectedPct() >= 1.0)
            return Pledge.PledgeStatus.COMPLETE;
        if(p.getCompletionScore() >= -0.04) // About 2 weeks grace period to account for vacations, etc.
            return Pledge.PledgeStatus.CURRENT;
        if(p.getCompletionScore() >= -0.082) // About a month
            return Pledge.PledgeStatus.SLIGHTLY_BEHIND;
        if(p.getCollectedAmount() > 0.0)
            return Pledge.PledgeStatus.BEHIND;

        return Pledge.PledgeStatus.NOT_STARTED;
    }
}
