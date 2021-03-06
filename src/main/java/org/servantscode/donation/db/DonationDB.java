package org.servantscode.donation.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.servantscode.commons.StringUtils.isSet;


public class DonationDB extends EasyDB<Donation> {
    private static final Logger LOG = LogManager.getLogger(DonationDB.class);

    private static final HashMap<String, String> FIELD_MAP = new HashMap<>(8);

    static {
        FIELD_MAP.put("fundName", "f.name");
        FIELD_MAP.put("donationDate", "date");
        FIELD_MAP.put("donationType", "type");
        FIELD_MAP.put("familyName", "fam.surname");
    }

    public DonationDB() {
        super(Donation.class, "familyName", FIELD_MAP);
    }

    public int getDonationCount(String search) {
        QueryBuilder query = select(count()).search(searchParser.parse(search));
        return getCount(query);
    }

    public float getDonationTotal(String search) {
        QueryBuilder query = select("sum(amount) AS total").search(searchParser.parse(search));
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
        QueryBuilder query = select(all()).search(searchParser.parse(search))
                .page(sortField, start, count);
        return get(query);
    }

    private static QueryBuilder all() {
        return DBAccess.select("d.*","f.name AS fund_name", "fam.surname AS family_name", "p.name AS recorder_name");
    }

    protected static QueryBuilder select(String... fields) {
        return select(DBAccess.select(fields));
    }

    private static QueryBuilder select(QueryBuilder select) {
        return select.from("donations d")
                .leftJoin("funds f on d.fund_id=f.id")
                .leftJoin("families fam on d.family_id=fam.id")
                .leftJoin("people p on d.recorder_id=p.id")
                .inOrg("d.org_id");
    }

    public int getFamilyDonationCount(int familyId, String search) {
        QueryBuilder query = select(count()).with("d.family_id", familyId)
                .search(searchParser.parse(search));
        return getCount(query);
    }

    public List<Donation> getAnnualDonations(int familyId, int year) {
        return get(select(all()).with("d.family_id", familyId).where("date_part('year', date) = ?", year));
    }

    public float getFamilyDonationTotal(int familyId, String search) {
        QueryBuilder query = select("sum(amount) AS total").with("d.family_id", familyId)
                .search(searchParser.parse(search));
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
        QueryBuilder query = select(all()).with("d.family_id", familyId)
                .search(searchParser.parse(search))
                .page(sortField, start, count);
        return get(query);
    }

    public List<Integer> getContributingFamilies(int year) {
        LocalDate startDate = LocalDate.of(year, 1,1);
        LocalDate endDate = LocalDate.of(year, 12,31);
        QueryBuilder query = DBAccess.select("DISTINCT family_id").from("donations")
                .where("date >= ?", startDate).where("date <= ?", endDate).inOrg();

        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            List<Integer> families = new LinkedList<>();
            while(rs.next())
                families.add(rs.getInt(1));

            return families;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get list of families with available donation data.", e);
        }
    }

    public List<Integer> getDonationYears(int familyId) {
        QueryBuilder query = select("DISTINCT EXTRACT('year' FROM date)").with("d.family_id", familyId).inOrg("d.org_id");
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()){

            ArrayList<Integer> years = new ArrayList<>(10);
            while(rs.next())
                years.add(rs.getInt(1));

            years.sort(Comparator.comparingInt(a -> -a));
            return years;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get list of years with available donation data.", e);
        }
    }

    public Donation getLastDonation(int familyId, int fundId) {
        QueryBuilder query = select(all()).with("d.family_id", familyId).with("fund_id", fundId)
                .sort("date DESC, recorded_time DESC").limit(1);
        return getOne(query);
    }

    public StreamingOutput getReportReader(String search, final List<String> fields) {
        QueryBuilder query = select(all()).search(searchParser.parse(search));
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
                .value("pledge_id", donation.getPledgeId())
                .value("amount", donation.getAmount())
                .value("deductible_amount", donation.getDeductibleAmount())
                .value("date", convert(donation.getDonationDate()))
                .value("type", stringify(donation.getDonationType()))
                .value("check_number", donation.getCheckNumber())
                .value("transaction_id", donation.getTransactionId())
                .value("batch_number", donation.getBatchNumber())
                .value("notes", donation.getNotes())
                .value("recorded_time", convert(donation.getRecordedTime()))
                .value("recorder_id", donation.getRecorderId())
                .value("org_id", OrganizationContext.orgId());
        donation.setId(createAndReturnKey(cmd));
        return donation;
    }

    public Donation createDonationIfUnique(Donation donation) {
        QueryBuilder query = select(all())
                .with("d.family_id", donation.getFamilyId())
                .with("d.fund_id", donation.getFundId())
                .with("amount", donation.getAmount())
                .with("deductible_amount", donation.getDeductibleAmount())
                .with("date", convert(donation.getDonationDate()))
                .with("type", stringify(donation.getDonationType()));

        if(isSet(donation.getTransactionId()))
            query.with("transaction_id", donation.getTransactionId());

        if(donation.getBatchNumber() > 0)
                query.with("batch_number", donation.getBatchNumber());
        if(donation.getCheckNumber() > 0)
                query.with("check_number", donation.getCheckNumber());

//        LOG.debug("Looking up donation with sql: " + query.getSql());

        Donation dbDonation = getOne(query);
        if(dbDonation != null)
            LOG.debug("Found existing matching donation.");
        return dbDonation != null? dbDonation: createDonation(donation);
    }

    public boolean updateDonation(Donation donation) {
        UpdateBuilder cmd = update("donations")
                .value("family_id", donation.getFamilyId())
                .value("fund_id", donation.getFundId())
                .value("pledge_id", donation.getPledgeId())
                .value("amount", donation.getAmount())
                .value("deductible_amount", donation.getDeductibleAmount())
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
        donation.setPledgeId(rs.getInt("pledge_id"));
        donation.setAmount(rs.getFloat("amount"));
        donation.setDeductibleAmount(rs.getFloat("deductible_amount"));
        donation.setDonationDate(convert(rs.getDate("date")));
        donation.setDonationType(rs.getString("type"));
        donation.setCheckNumber(rs.getLong("check_number"));
        donation.setTransactionId(rs.getString("transaction_id"));
        donation.setNotes(rs.getString("notes"));
        donation.setRecordedTime(convert(rs.getTimestamp("recorded_time")));
        donation.setRecorderId(rs.getInt("recorder_id"));
        donation.setRecorderName(rs.getString("recorder_name"));
        return donation;
    }
}
