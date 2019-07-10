package org.servantscode.donation.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Fund;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isSet;

public class FundDB extends DBAccess {

    private SearchParser<Fund> searchParser;

    public FundDB() {
        searchParser = new SearchParser<>(Fund.class);
    }

    public int getFundCount(String search) {
        QueryBuilder query = count().from("funds").search(searchParser.parse(search)).inOrg();
//        String sql = format("SELECT count(1) FROM funds%s",
//                optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

             return rs.next()? rs.getInt(1): 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve fund count.", e);
        }
    }

    public List<Fund> getFunds(int start, int count, String sortField, String search) {
        QueryBuilder query = selectAll().from("funds").search(searchParser.parse(search)).inOrg()
                .sort(sortField).limit(count).offset(start);
//        String sql = format("SELECT * FROM funds%s ORDER BY %s LIMIT ? OFFSET ?",
//                optionalWhereClause(search),
//                sortField);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)) {

            return processFundResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve funds.", e);
        }
    }

    public Fund getFund(int id) {
        QueryBuilder query = selectAll().from("funds").withId(id).inOrg();
//        String sql = "SELECT * FROM funds WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn) ) {

            return firstOrNull(processFundResults(stmt));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve fund: " + id, e);
        }
    }

    public Fund createFund(Fund fund) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO funds (name, org_id) VALUES (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setString(1, fund.getName());
            stmt.setInt(2, OrganizationContext.orgId());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create fund: " + fund.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    fund.setId(rs.getInt(1));
            }
            return fund;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create fund: " + fund.getName(), e);
        }
    }

    public boolean updateFund(Fund fund) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE funds SET name=? WHERE id=? AND org_id=?")
        ) {
            stmt.setString(1, fund.getName());
            stmt.setInt(2, fund.getId());
            stmt.setInt(3, OrganizationContext.orgId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update fund: " + fund.getName(), e);
        }
    }

    public boolean deleteFund(int fundId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM funds WHERE id=? AND org_id=?")
        ) {
            stmt.setInt(1, fundId);
            stmt.setInt(2, OrganizationContext.orgId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete fund with id: " + fundId, e);
        }
    }

    // ----- Private -----
    private List<Fund> processFundResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Fund> funds = new ArrayList<>();
            while(rs.next()) {
                Fund fund = new Fund();
                fund.setId(rs.getInt("id"));
                fund.setName(rs.getString("name"));
                funds.add(fund);
            }
            return funds;
        }
    }

    private String optionalWhereClause(String search) {
        return isSet(search) ? format(" WHERE name ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }
}

