package org.servantscode.donation.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.donation.Pledge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PledgeDB extends DBAccess {
    public Pledge getActivePledge(int familyId) {
        String sql = "SELECT p.*, f.envelope_number from pledges p, families f WHERE p.family_id = f.id AND family_id=? AND pledge_start < NOW() and pledge_end > NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);

            List<Pledge> results = processPledgeResults(stmt);
            if(results.isEmpty())
                return null;

            return results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve pledge for family: " + familyId, e);
        }
    }

    public Pledge getActivePledgeByEnvelope(int envelopeNumber) {
        String sql = "SELECT p.*, f.envelope_number from pledges p, families f WHERE p.family_id = f.id AND envelope_number=? AND pledge_start < NOW() and pledge_end > NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, envelopeNumber);

            List<Pledge> results = processPledgeResults(stmt);
            if(results.isEmpty())
                return null;

            return results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve pledge for envelope: " + envelopeNumber, e);
        }
    }

    public List<Pledge> getFamilyPledges(int familyId) {
        String sql = "SELECT p.*, f.envelope_number from pledges p, families f WHERE p.family_id = f.id AND family_id=? ORDER BY pledge_end DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);

            return processPledgeResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve pledge for family: " + familyId, e);
        }
    }

    public Pledge createPledge(Pledge pledge) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO pledges " +
                          "(family_id, pledge_type, pledge_date, pledge_start, pledge_end, frequency, pledge_increment, total_pledge) " +
                          "VALUES (?,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setInt(1, pledge.getFamilyId());
            stmt.setString(2, pledge.getPledgeType().toString());
            stmt.setTimestamp(3, convert(pledge.getPledgeDate()));
            stmt.setTimestamp(4, convert(pledge.getPledgeStart()));
            stmt.setTimestamp(5, convert(pledge.getPledgeEnd()));
            stmt.setString(6, pledge.getPledgeFrequency().toString());
            stmt.setFloat(7, pledge.getPledgeAmount());
            stmt.setFloat(8, pledge.getAnnualPledgeAmount());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not store donation for family: " + pledge.getFamilyId());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    pledge.setId(rs.getInt(1));
            }
            return pledge;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create pledge for family: " + pledge.getFamilyId(), e);
        }
    }

    public boolean updatePledge(Pledge pledge) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE pledges SET " +
                          "family_id=?, pledge_type=?, pledge_date=?, pledge_start=?, pledge_end=?, frequency=?, pledge_increment=?, total_pledge=? " +
                          "WHERE id=?")
        ) {
            stmt.setInt(1, pledge.getFamilyId());
            stmt.setString(2, pledge.getPledgeType().toString());
            stmt.setTimestamp(3, convert(pledge.getPledgeDate()));
            stmt.setTimestamp(4, convert(pledge.getPledgeStart()));
            stmt.setTimestamp(5, convert(pledge.getPledgeEnd()));
            stmt.setString(6, pledge.getPledgeFrequency().toString());
            stmt.setFloat(7, pledge.getPledgeAmount());
            stmt.setFloat(8, pledge.getAnnualPledgeAmount());
            stmt.setInt(9, pledge.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update pledge with id: " + pledge.getId(), e);
        }
    }

    public boolean deletePledge(int pledgeId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM pledges WHERE id=?")
        ) {
            stmt.setInt(1, pledgeId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete pledge with id: " + pledgeId, e);
        }
    }

    //TODO: This should really be a service call, not a cross service DB lookup, but I'm leaving it as is until
    //      production network architecture is sorted out. [Greg]
    public String getFamilySurname(int familyId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement( "SELECT surname FROM families WHERE id=?")
        ) {
            stmt.setInt(1, familyId);

            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next())
                    return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find family with id: " + familyId, e);
        }
    }

    // ----- Private -----
    private List<Pledge> processPledgeResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Pledge> pledges = new ArrayList<>();
            while(rs.next()) {
                Pledge pledge = new Pledge();
                pledge.setId(rs.getInt("id"));
                pledge.setFamilyId(rs.getInt("family_id"));
                pledge.setPledgeType(rs.getString("pledge_type"));
                pledge.setEnvelopeNumber(rs.getInt("envelope_number"));
                pledge.setPledgeDate(convert(rs.getTimestamp("pledge_date")));
                pledge.setPledgeStart(convert(rs.getTimestamp("pledge_start")));
                pledge.setPledgeEnd(convert(rs.getTimestamp("pledge_end")));
                pledge.setPledgeFrequency(rs.getString("frequency"));
                pledge.setPledgeAmount(rs.getFloat("pledge_increment"));
                pledge.setAnnualPledgeAmount(rs.getFloat("total_pledge"));
                pledges.add(pledge);
            }
            return pledges;
        }
    }
}
