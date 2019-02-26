package org.servantscode.donation.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.donation.FamilyGivingInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FamilyGivingInfoDB extends DBAccess {

    //TODO: This should really be a service call, not a cross service DB lookup, but I'm leaving it as is until
    //      production network architecture is sorted out. [Greg]

    public FamilyGivingInfo getFamilyPledgeByEnvelope(int envelopeNumber) {
        String sql = "SELECT id, surname, envelope_number FROM families WHERE envelope_number=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, envelopeNumber);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve family info for envelope: " + envelopeNumber, e);
        }
    }

    public FamilyGivingInfo getFamilyPledgeById(int familyId) {
        String sql = "SELECT id, surname, envelope_number FROM families WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve family info for id: " + familyId, e);
        }
    }

    // ----- Private -----
    private FamilyGivingInfo processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            if(rs.next()) {
                FamilyGivingInfo pledge = new FamilyGivingInfo();
                pledge.setId(rs.getInt("id"));
                pledge.setSurname(rs.getString("surname"));
                pledge.setEnvelopeNumber(rs.getInt("envelope_number"));
                return pledge;
            }
            return null;
        }
    }
}
