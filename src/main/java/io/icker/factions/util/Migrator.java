package io.icker.factions.util;

import java.sql.*;
import java.util.UUID;

import com.sun.jna.platform.unix.Resource;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.*;
import io.icker.factions.api.persistents.User.*;
import io.icker.factions.api.persistents.Relationship.Status;
import net.minecraft.util.Formatting;

public class Migrator {
    public static Connection con;
    public Migrator() {
        try {
            con = DriverManager.getConnection("jdbc:h2:./factions/factions");

            Query query = new Query("SELECT * FROM Faction;").executeQuery();
            while (query.next()) {
                Faction faction = new Faction(query.getString("name"), query.getString("description"), Formatting.byName(query.getString("color")), query.getBool("open"), query.getInt("power"));
                Faction.add(faction);

                Query homeQuery = new Query("SELECT * FROM Home WHERE faction = ?;").set(faction.getName()).executeQuery();
                if (homeQuery.success) {
                    Home home = new Home(faction.getID(), homeQuery.getDouble("x"), homeQuery.getDouble("y"), homeQuery.getDouble("z"), homeQuery.getFloat("yaw"), homeQuery.getFloat("pitch"), homeQuery.getString("level"));
                    Home.set(home);
                }

                Query claimQuery = new Query("SELECT * FROM Claim WHERE faction = ?;").set(faction.getName()).executeQuery();
                while (claimQuery.next()) {
                    Claim claim = new Claim(claimQuery.getInt("x"), claimQuery.getInt("z"), claimQuery.getString("level"), faction.getID());
                    Claim.add(claim);
                }

                Query inviteQuery = new Query("SELECT * FROM Invite WHERE faction = ?;").set(faction.getName()).executeQuery();
                while (inviteQuery.next()) {
                    Invite invite = new Invite(inviteQuery.getUUID("player"), faction.getID());
                    Invite.add(invite);
                }
            }

            query = new Query("SELECT * FROM Member;").executeQuery();
            while (query.next()) {
                OldRank rank;
                try {
                    rank = Enum.valueOf(OldRank.class, query.getString("rank"));
                } catch (IllegalArgumentException e) {
                    rank = OldRank.CIVILIAN;
                }

                User user = new User(query.getUUID("uuid"), ChatMode.GLOBAL, false, false);
                user.joinFaction(Faction.getByName(query.getString("faction")).getID(), migrateRank(rank));
                User.add(user);
            }

            query = new Query("SELECT * FROM PlayerConfig;").executeQuery();
            while (query.next()) {
                ChatMode opt;
                try {
                    opt = Enum.valueOf(ChatMode.class, query.getString("chat"));
                } catch (IllegalArgumentException e) {
                    opt = ChatMode.GLOBAL;
                }

                User user = User.get(query.getUUID("uuid"));
                user.setBypass(query.getBool("bypass"));
                user.setChatMode(opt);
                user.setZoneMessage(query.getBool("zone"));
            }

            query = new Query("SELECT * FROM Allies;").executeQuery();
            while (query.next()) {
                Relationship rel = new Relationship(Faction.getByName(query.getString("source")).getID(), Faction.getByName(query.getString("target")).getID(), Status.ALLY);
                Relationship.set(rel);

                if (query.getBool("accept")) {
                    rel = new Relationship(Faction.getByName(query.getString("target")).getID(), Faction.getByName(query.getString("source")).getID(), Status.ALLY);
                    Relationship.set(rel);
                }
            }
        } catch (SQLException err) {
            FactionsMod.LOGGER.error("An error occurred during data migration", err);
        }

        try {
            con.close();
        } catch (SQLException err) {
            FactionsMod.LOGGER.error("An error occurred during data migration", err);
        }
    }

    private Rank migrateRank(OldRank rank) {
        switch (rank) {
            case OWNER -> {
                return Rank.OWNER;
            }
            case CO_OWNER -> {
                return Rank.LEADER;
            }
            case OFFICER -> {
                return Rank.COMMANDER;
            }
            case CIVILIAN -> {
                return Rank.MEMBER;
            }
        }

        return Rank.MEMBER;
    }

    public enum OldRank {
        OWNER,
        CO_OWNER,
        OFFICER,
        CIVILIAN
    }

    private static class Query {
        private PreparedStatement statement;
        private ResultSet result;

        private final String query;
        private int paramIndex = 1;
        private boolean skippedNext = false;

        boolean success;

        public Query(String query) {
            this.query = query;
            try {
                statement = con.prepareStatement(query);
            } catch (SQLException e) {
                error(e);
            }
        }

        public Query set(Object... items) {
            try {
                for (Object item : items) {
                    statement.setObject(paramIndex, item);
                    paramIndex++;
                }
            } catch (SQLException e) {
                error(e);
            }
            return this;
        }

        public String getString(String columnName) {
            try {
                return result.getString(columnName);
            } catch (SQLException e) {
                error(e);
            }
            return null;
        }

        public UUID getUUID(String columnName) {
            try {
                return result.getObject(columnName, UUID.class);
            } catch (SQLException e) {
                error(e);
            }
            return null;
        }

        public int getInt(String columnName) {
            try {
                return result.getInt(columnName);
            } catch (SQLException e) {
                error(e);
            }
            return 0;
        }

        public double getDouble(String columnName) {
            try {
                return result.getDouble(columnName);
            } catch (SQLException e) {
                error(e);
            }
            return 0;
        }

        public float getFloat(String columnName) {
            try {
                return result.getFloat(columnName);
            } catch (SQLException e) {
                error(e);
            }
            return 0;
        }

        public boolean getBool(String columnName) {
            try {
                return result.getBoolean(columnName);
            } catch (SQLException e) {
                error(e);
            }
            return false;
        }

        public Object getObject(String columnName) {
            try {
                return result.getObject(columnName);
            } catch (SQLException e) {
                error(e);
            }
            return false;
        }


        public Query executeUpdate() {
            try {
                int affectedRows = statement.executeUpdate();
                success = affectedRows != 0;
            } catch (SQLException e) {
                error(e);
            }
            return this;
        }

        public Query executeQuery() {
            try {
                result = statement.executeQuery();
                success = result.next();
            } catch (SQLException e) {
                error(e);
            }
            return this;
        }

        public boolean next() {
            try {
                if (skippedNext) return result.next();
            } catch (SQLException e) {
                error(e);
            }
            skippedNext = true;
            return success;
        }

        public boolean exists() {
            try {
                return result.getBoolean(1);
            } catch (SQLException e) {
                error(e);
            }
            return false;
        }

        private void error(SQLException e) {
            FactionsMod.LOGGER.error("Error executing database transaction {}", query, e);
        }
    }
}
