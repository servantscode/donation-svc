package org.servantscode.donation.db;

import org.servantscode.commons.StringUtils;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.donation.Donation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class DonationDB extends DBAccess {
    public int getDonationCount(int familyId, String search) {
        String sql = format("SELECT count(1) FROM donations WHERE family_id=? %s",
                optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);

            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve donations for family: " + familyId, e);
        }
        return 0;
    }

    public List<Donation> getFamilyDonations(int familyId, int start, int count, String sortField, String search) {
        String sql = format("SELECT d.*, f.name FROM donations d, funds f WHERE d.fund_id = f.id AND family_id=? %s ORDER BY %s LIMIT ? OFFSET ?",
                optionalWhereClause(search),
                sortField);
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);
            stmt.setInt(2, count);
            stmt.setInt(3, start);

            return processDonationResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve donations for family: " + familyId, e);
        }
    }

    public Donation getLastDonation(int familyId, int fundId) {
        String sql = "SELECT d.*, f.name FROM donations d, funds f WHERE d.fund_id = f.id AND family_id=? AND fund_id=? ORDER BY date DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);
            stmt.setInt(2, fundId);

            List<Donation> donations = processDonationResults(stmt);
            if(donations.isEmpty())
                return null;

            return donations.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve donations for family: " + familyId, e);
        }
    }

    public Donation createDonation(Donation donation) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO donations " +
                          "(family_id, fund_id, amount, date, type, check_number, transaction_id) " +
                          "VALUES (?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setInt(1, donation.getFamilyId());
            stmt.setInt(2, donation.getFundId());
            stmt.setFloat(3, donation.getAmount());
            stmt.setTimestamp(4, convert(donation.getDonationDate()));
            stmt.setString(5, donation.getDonationType().toString());
            stmt.setInt(6, donation.getCheckNumber());
            stmt.setLong(7, donation.getTransactionId());

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
                          "WHERE id=?")
        ) {

            stmt.setInt(1, donation.getFamilyId());
            stmt.setInt(2, donation.getFundId());
            stmt.setFloat(3, donation.getAmount());
            stmt.setTimestamp(4, convert(donation.getDonationDate()));
            stmt.setString(5, donation.getDonationType().toString());
            stmt.setInt(6, donation.getCheckNumber());
            stmt.setLong(7, donation.getTransactionId());
            stmt.setLong(8, donation.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update donation with id: " + donation.getId(), e);
        }
    }

    public boolean deleteDonation(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM donations WHERE id=?")
        ) {
            stmt.setInt(1, id);

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
                donation.setDonationDate(convert(rs.getTimestamp("date")));
                donation.setDonationType(rs.getString("type"));
                donation.setCheckNumber(rs.getInt("check_number"));
                donation.setTransactionId(rs.getLong("transaction_id"));
                donations.add(donation);
            }
            return donations;
        }
    }

    private String optionalWhereClause(String search) {
        //TODO: Fill this in with advanced search capabilities
//        return !isEmpty(search) ? format(" AND p.name ILIKE '%%%s%%'", search.replace("'", "''")) : "";
        return "";
    }
}
