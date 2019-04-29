package org.servantscode.donation.db;

import org.servantscode.commons.db.DBAccess;
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

    public int getFundCount(String search) {
        String sql = format("SELECT count(1) FROM funds%s",
                optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve fund count.", e);
        }
        return 0;
    }

    public List<Fund> getFunds(int start, int count, String sortField, String search) {
        String sql = format("SELECT * FROM funds%s ORDER BY %s LIMIT ? OFFSET ?",
                optionalWhereClause(search),
                sortField);
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

            return processFundResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve funds.", e);
        }
    }

    public Fund getFund(int id) {
        String sql = "SELECT * FROM funds WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, id);

            List<Fund> results = processFundResults(stmt);
            if(results.isEmpty())
                return null;

            return results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve fund: " + id, e);
        }
    }

    public Fund createFund(Fund fund) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO funds (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setString(1, fund.getName());

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
                     "UPDATE funds SET name=? WHERE id=?")
        ) {
            stmt.setString(1, fund.getName());
            stmt.setInt(2, fund.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update fund: " + fund.getName(), e);
        }
    }

    public boolean deleteFund(int fundId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM funds WHERE id=?")
        ) {
            stmt.setInt(1, fundId);

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

