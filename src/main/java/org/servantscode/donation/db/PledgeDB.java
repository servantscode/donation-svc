package org.servantscode.donation.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.donation.Pledge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class PledgeDB extends DBAccess {

    public int getActivePledgeCount(String search, int fundId) {
        String sql = format("SELECT count(1) FROM pledges %s",
                whereClause(search, fundId));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve active pledges.", e);
        }
        return 0;
    }

    public List<Pledge> getActivePledges(int start, int count, String sortField, String search, int fundId) {
        String sql = format("SELECT p.*, f.name FROM pledges p, funds f WHERE f.id=p.fund_id%s ORDER BY %s LIMIT ? OFFSET ?",
                additionalWhereClause(search, fundId),
                sortField);
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

            return processPledgeResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve active pledges.", e);
        }
    }

    public Pledge getActivePledge(int familyId, int fundId) {
        String sql = "SELECT p.*, f.name FROM pledges p, funds f WHERE f.id=p.fund_id AND family_id=? AND fund_id=? AND pledge_start < NOW() and pledge_end > NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);
            stmt.setInt(2, fundId);

            List<Pledge> results = processPledgeResults(stmt);
            if(results.isEmpty())
                return null;

            return results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve pledge for family: " + familyId, e);
        }
    }

    public List<Pledge> getActiveFamilyPledges(int familyId) {
        String sql = "SELECT p.*, f.name FROM pledges p, funds f WHERE f.id=p.fund_id AND family_id=? AND pledge_start < NOW() and pledge_end > NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, familyId);

            return processPledgeResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve pledge for family: " + familyId, e);
        }
    }

    public List<Pledge> getFamilyPledges(int familyId) {
        String sql = "SELECT p.*, f.name FROM pledges p, funds f WHERE f.id=p.fund_id AND family_id=? ORDER BY pledge_end DESC";
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
                          "(family_id, fund_id, pledge_type, pledge_date, pledge_start, pledge_end, frequency, pledge_increment, total_pledge) " +
                          "VALUES (?,?,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setInt(1, pledge.getFamilyId());
            stmt.setInt(2, pledge.getFundId());
            stmt.setString(3, pledge.getPledgeType().toString());
            stmt.setTimestamp(4, convert(pledge.getPledgeDate()));
            stmt.setTimestamp(5, convert(pledge.getPledgeStart()));
            stmt.setTimestamp(6, convert(pledge.getPledgeEnd()));
            stmt.setString(7, pledge.getPledgeFrequency().toString());
            stmt.setFloat(8, pledge.getPledgeAmount());
            stmt.setFloat(9, pledge.getAnnualPledgeAmount());

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
                          "family_id=?, fund_id=?, pledge_type=?, pledge_date=?, pledge_start=?, pledge_end=?, frequency=?, pledge_increment=?, total_pledge=? " +
                          "WHERE id=?")
        ) {
            stmt.setInt(1, pledge.getFamilyId());
            stmt.setInt(2, pledge.getFundId());
            stmt.setString(3, pledge.getPledgeType().toString());
            stmt.setTimestamp(4, convert(pledge.getPledgeDate()));
            stmt.setTimestamp(5, convert(pledge.getPledgeStart()));
            stmt.setTimestamp(6, convert(pledge.getPledgeEnd()));
            stmt.setString(7, pledge.getPledgeFrequency().toString());
            stmt.setFloat(8, pledge.getPledgeAmount());
            stmt.setFloat(9, pledge.getAnnualPledgeAmount());
            stmt.setInt(10, pledge.getId());

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

    // ----- Private -----
    private List<Pledge> processPledgeResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Pledge> pledges = new ArrayList<>();
            while(rs.next()) {
                Pledge pledge = new Pledge();
                pledge.setId(rs.getInt("id"));
                pledge.setFamilyId(rs.getInt("family_id"));
                pledge.setFundId(rs.getInt("fund_id"));
                pledge.setFundName(rs.getString("name"));
                pledge.setPledgeType(rs.getString("pledge_type"));
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

    private String whereClause(String search, int fundId) {
        //TODO: Fill this in with advanced search capabilities
        if(fundId == 0)
            return "";
        return " WHERE fund_id=" + fundId;
    }

    private String additionalWhereClause(String search, int fundId) {
        //TODO: Fill this in with advanced search capabilities
        if(fundId == 0)
            return "";
        return " AND fund_id=" + fundId;
    }
}
