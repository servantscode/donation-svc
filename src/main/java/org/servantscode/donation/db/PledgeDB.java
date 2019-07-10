package org.servantscode.donation.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Pledge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class PledgeDB extends DBAccess {

    private SearchParser<Pledge> searchParser;

    public PledgeDB() {
        this.searchParser = new SearchParser<>(Pledge.class);
    }

    public int getActivePledgeCount(String search, int fundId) {

        QueryBuilder query = count().from("pledges").search(searchParser.parse(search)).where("fund_id=?", fundId).inOrg();
//        String sql = format("SELECT count(1) FROM pledges %s",
//                whereClause(search, fundId));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next()? rs.getInt(1): 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve active pledges.", e);
        }
    }

    private QueryBuilder baseQuery() {
        return select("p.*", "f.name").from("pledges p", "funds f").where("f.id=p.fund_id");
    }

    public List<Pledge> getActivePledges(int start, int count, String sortField, String search, int fundId) {
        QueryBuilder query = baseQuery().search(searchParser.parse(search)).where("fund_id=?", fundId).inOrg("p.org_id")
                .sort(sortField).limit(count).offset(start);
//        String sql = format("SELECT p.*, f.name FROM pledges p, funds f WHERE f.id=p.fund_id%s ORDER BY %s LIMIT ? OFFSET ?",
//                additionalWhereClause(search, fundId),
//                sortField);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)) {

            return processPledgeResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve active pledges.", e);
        }
    }

    public Pledge getActivePledge(int familyId, int fundId) {
        QueryBuilder query = baseQuery().where("family_id=?", familyId).where("fund_id=?", fundId)
                .where("pledge_start < NOW() AND pledge_end > NOW()");
//        String sql = "SELECT p.*, f.name FROM pledges p, funds f WHERE f.id=p.fund_id AND family_id=? AND fund_id=? AND pledge_start < NOW() and pledge_end > NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn) ) {

            return firstOrNull(processPledgeResults(stmt));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve pledge for family: " + familyId, e);
        }
    }

    public List<Pledge> getActiveFamilyPledges(int familyId) {
        QueryBuilder query = baseQuery().where("family_id=?", familyId)
                .where("pledge_start < NOW() AND pledge_end > NOW()");
//        String sql = "SELECT p.*, f.name FROM pledges p, funds f WHERE f.id=p.fund_id AND family_id=? AND pledge_start < NOW() and pledge_end > NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)) {

            return processPledgeResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve pledge for family: " + familyId, e);
        }
    }

    public List<Pledge> getFamilyPledges(int familyId) {
        QueryBuilder query = baseQuery().where("family_id=?", familyId)
                .sort("pledge_end DESC");
//        String sql = "SELECT p.*, f.name FROM pledges p, funds f WHERE f.id=p.fund_id AND family_id=? ORDER BY pledge_end DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)) {

            return processPledgeResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve pledge for family: " + familyId, e);
        }
    }

    public Pledge createPledge(Pledge pledge) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO pledges " +
                          "(family_id, fund_id, pledge_type, pledge_date, pledge_start, pledge_end, frequency, pledge_increment, total_pledge, org_id) " +
                          "VALUES (?,?,?,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setInt(1, pledge.getFamilyId());
            stmt.setInt(2, pledge.getFundId());
            stmt.setString(3, pledge.getPledgeType().toString());
            stmt.setDate(4, convert(pledge.getPledgeDate()));
            stmt.setDate(5, convert(pledge.getPledgeStart()));
            stmt.setDate(6, convert(pledge.getPledgeEnd()));
            stmt.setString(7, pledge.getPledgeFrequency().toString());
            stmt.setFloat(8, pledge.getPledgeAmount());
            stmt.setFloat(9, pledge.getAnnualPledgeAmount());
            stmt.setInt(10, OrganizationContext.orgId());

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
                          "WHERE id=? AND org_id=?")
        ) {
            stmt.setInt(1, pledge.getFamilyId());
            stmt.setInt(2, pledge.getFundId());
            stmt.setString(3, pledge.getPledgeType().toString());
            stmt.setDate(4, convert(pledge.getPledgeDate()));
            stmt.setDate(5, convert(pledge.getPledgeStart()));
            stmt.setDate(6, convert(pledge.getPledgeEnd()));
            stmt.setString(7, pledge.getPledgeFrequency().toString());
            stmt.setFloat(8, pledge.getPledgeAmount());
            stmt.setFloat(9, pledge.getAnnualPledgeAmount());
            stmt.setInt(10, pledge.getId());
            stmt.setInt(11, OrganizationContext.orgId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update pledge with id: " + pledge.getId(), e);
        }
    }

    public boolean deletePledge(int pledgeId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM pledges WHERE id=? AND org_id=?")
        ) {
            stmt.setInt(1, pledgeId);
            stmt.setInt(2, OrganizationContext.orgId());

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
                pledge.setPledgeDate(convert(rs.getDate("pledge_date")));
                pledge.setPledgeStart(convert(rs.getDate("pledge_start")));
                pledge.setPledgeEnd(convert(rs.getDate("pledge_end")));
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
