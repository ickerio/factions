package io.icker.factions.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.icker.factions.FactionsMod;

public class Query {
    private PreparedStatement statement;;
    private ResultSet result;
    private int paramIndex = 1;
    boolean success;
    String query;

    public Query(String query) {
        this.query = query;
        try {
            statement = Database.con.prepareStatement(query);
        } catch (SQLException e) {error();}
    }

    public Query set(Object ...items) {
        try {
            for (Object item : items) {
                statement.setObject(paramIndex, item);
                paramIndex++;
            }
        } catch (SQLException e) {error();}
        return this;
    }

    public String getString(String columnName) {
        try {
            return result.getString(columnName);
        } catch (SQLException e) {error();}
        return null;
    }

    public int getInt(String columnName) {
        try {
            return result.getInt(columnName);
        } catch (SQLException e) {error();}
        return 0;
    }

    public boolean getBool(String columnName) {
        try {
            return result.getBoolean(columnName);
        } catch (SQLException e) {error();}
        return false;
    }

    public Query executeUpdate() {
        try {
            int affectedRows = statement.executeUpdate();
            success = affectedRows != 0;
        } catch (SQLException e) {error();}
        return this;
    }

    public Query executeQuery() {
        try {
            result = statement.executeQuery();
            success = result.next();
        } catch (SQLException e) {error();}
        return this;
    }

    private void error() {
        FactionsMod.LOGGER.error("Error executing database transaction {}", query);
    }
}