package org.servantscode.donation.db;

import org.servantscode.commons.StringUtils;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Donation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class DonationDB extends DBAccess {

    private SearchParser<Donation> searchParser;

    private static final HashMap<String, String> FIELD_MAP = new HashMap<>(8);

    static {
        FIELD_MAP.put("fundName", "f.name");
        FIELD_MAP.put("donationDate", "date");
        FIELD_MAP.put("donationType", "type");
    }

    public DonationDB() {
        searchParser = new SearchParser<>(Donation.class, "fundName", FIELD_MAP);
    }

    public int getDonationCount(int familyId, String search) {
        QueryBuilder query = count().from("donations d","funds f").where("d.fund_id=f.id")
                .where("family_id=?", familyId).search(searchParser.parse(search)).inOrg("d.org_id");
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve donations for family: " + familyId, e);
        }
    }

    private QueryBuilder baseQuery() {
        return select("d.*","f.name").from("donations d","funds f").where("d.fund_id=f.id").inOrg("d.org_id");
    }

    public List<Donation> getFamilyDonations(int familyId, int start, int count, String sortField, String search) {
        QueryBuilder query = baseQuery().where("family_id=?", familyId).search(searchParser.parse(search))
                .sort(sortField).limit(count).offset(start);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn) ) {

            return processDonationResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve donations for family: " + familyId, e);
        }
    }

    public Donation getLastDonation(int familyId, int fundId) {
        QueryBuilder query = baseQuery().where("family_id=?", familyId).where("fund_id=?", fundId).sort("date DESC").limit(1);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)) {

            return firstOrNull(processDonationResults(stmt));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve donations for family: " + familyId, e);
        }
    }

    public Donation createDonation(Donation donation) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO donations " +
                          "(family_id, fund_id, amount, date, type, check_number, transaction_id, org_id) " +
                          "VALUES (?,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setInt(1, donation.getFamilyId());
            stmt.setInt(2, donation.getFundId());
            stmt.setFloat(3, donation.getAmount());
            stmt.setDate(4, convert(donation.getDonationDate()));
            stmt.setString(5, donation.getDonationType().toString());
            stmt.setInt(6, donation.getCheckNumber());
            stmt.setLong(7, donation.getTransactionId());
            stmt.setInt(8, OrganizationContext.orgId());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not store donation for family: " + donation.getFamilyId());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    donation.setId(rs.getLong(1));
            }
            return donation;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create donation for family: " + donation.getFamilyId(), e);
        }
    }

    public boolean updateDonation(Donation donation) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE donations SET " +
                          "family_id=?, fund_id=?, amount=?, date=?, type=?, check_number=?, transaction_id=? " +
                          "WHERE id=? AND org_id=?")
        ) {

            stmt.setInt(1, donation.getFamilyId());
            stmt.setInt(2, donation.getFundId());
            stmt.setFloat(3, donation.getAmount());
            stmt.setDate(4, convert(donation.getDonationDate()));
            stmt.setString(5, donation.getDonationType().toString());
            stmt.setInt(6, donation.getCheckNumber());
            stmt.setLong(7, donation.getTransactionId());
            stmt.setLong(8, donation.getId());
            stmt.setInt(9, OrganizationContext.orgId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update donation with id: " + donation.getId(), e);
        }
    }

    public boolean deleteDonation(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM donations WHERE id=? AND org_id=?")
        ) {
            stmt.setInt(1, id);
            stmt.setInt(2, OrganizationContext.orgId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete donation with id: " + id, e);
        }
    }

    // ----- Private -----
    private List<Donation> processDonationResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Donation> donations = new ArrayList<>();
            while(rs.next()) {
                Donation donation = new Donation();
                donation.setId(rs.getLong("id"));
                donation.setFamilyId(rs.getInt("family_id"));
                donation.setFundId(rs.getInt("fund_id"));
                donation.setFundName(rs.getString("name"));
                donation.setAmount(rs.getFloat("amount"));
                donation.setDonationDate(convert(rs.getDate("date")));
                donation.setDonationType(rs.getString("type"));
                donation.setCheckNumber(rs.getInt("check_number"));
                donation.setTransactionId(rs.getLong("transaction_id"));
                donations.add(donation);
            }
            return donations;
        }
    }
}
