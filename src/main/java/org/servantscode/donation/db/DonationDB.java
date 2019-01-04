package org.servantscode.donation.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.donation.Donation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DonationDB extends DBAccess {
    public List<Donation> getFamilyDonations(int familyId) {
        String sql = "SELECT * FROM donations WHERE family_id=? ORDER BY date DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);

            return processDonationResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve donations for family: " + familyId, e);
        }
    }

    public Donation getLastDonation(int familyId) {
        String sql = "SELECT * FROM donations WHERE family_id=? ORDER BY date DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);

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
                          "(family_id, amount, date, type, check_number, transaction_id) " +
                          "VALUES (?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setInt(1, donation.getFamilyId());
            stmt.setFloat(2, donation.getAmount());
            stmt.setDate(3, convert(donation.getDonationDate()));
            stmt.setString(4, donation.getDonationType().toString());
            stmt.setInt(5, donation.getCheckNumber());
            stmt.setLong(6, donation.getTransactionId());

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
                          "family_id=?, amount=?, date=?, type=?, check_number=?, transaction_id=? " +
                          "WHERE id=?")
        ) {
            stmt.setInt(1, donation.getFamilyId());
            stmt.setFloat(2, donation.getAmount());
            stmt.setDate(3, convert(donation.getDonationDate()));
            stmt.setString(4, donation.getDonationType().toString());
            stmt.setInt(5, donation.getCheckNumber());
            stmt.setLong(6, donation.getTransactionId());
            stmt.setLong(7, donation.getId());

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
                donation.setAmount(rs.getFloat("amount"));
                donation.setDonationDate(rs.getTimestamp("date"));
                donation.setDonationType(rs.getString("type"));
                donation.setCheckNumber(rs.getInt("check_number"));
                donation.setTransactionId(rs.getLong("transaction_id"));
                donations.add(donation);
            }
            return donations;
        }
    }
}
