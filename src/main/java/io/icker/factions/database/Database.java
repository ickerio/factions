package io.icker.factions.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import io.icker.factions.FactionsMod;
import net.minecraft.util.Formatting;

public class Database {
    public static Connection con;

    public static void connect() {
        try {
            con = DriverManager.getConnection("jdbc:h2:./factions/factions");
            new Query("""
                CREATE TABLE IF NOT EXISTS Faction (
                    name VARCHAR(255) PRIMARY KEY,
                    description VARCHAR(255),
                    color VARCHAR(255),
                    open BOOLEAN,
                    power INTEGER
                );

                CREATE TABLE IF NOT EXISTS Member (
                    uuid UUID PRIMARY KEY,
                    faction VARCHAR(255),
                    FOREIGN KEY(faction) REFERENCES Faction(name) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS Claim (
                    x INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    level VARCHAR(255),
                    faction VARCHAR(255),
                    FOREIGN KEY(faction) REFERENCES Faction(name) ON DELETE CASCADE
                );""")
                .executeUpdate();
            FactionsMod.LOGGER.info("Successfully connected to database");
        } catch (SQLException e) {
            e.printStackTrace();    
            FactionsMod.LOGGER.error("Error connecting to and setting up database");
        }
    }

    public static void disconnect() {
        try {
            con.close();
            FactionsMod.LOGGER.info("Successfully disconnected from database");
        } catch (SQLException e) {
            FactionsMod.LOGGER.error("Error disconnecting from database");
        }
    }

    // TODO: stmt.close() on gets
    public class Factions {
        public static Faction get(String name) {
            Query query = new Query("SELECT * FROM Faction WHERE name = ?;")
                .set(name)
                .executeQuery();

            if (!query.success) return null;
            return new Faction(name, query.getString("description"), Formatting.byName(query.getString("color")), query.getBool("open"), query.getInt("power"));
        }

        public static Faction add(String name, String description, String color, boolean open, int power) {
            Query query = new Query("INSERT INTO Faction VALUES (?, ?, ?, ?, ?);")
                .set(name, description, color, open, power)
                .executeUpdate();

            if (!query.success) return null;
            return new Faction(name, description, Formatting.byName(color), open, power);
        }

        public static void remove(String name) {
            new Query("DELETE FROM Faction WHERE name = ?;")
                .set(name)
                .executeUpdate();
        }
    }

    public class Members {
        public static Member get(UUID uuid) {
            Query query = new Query("SELECT faction FROM Member WHERE uuid = ?;")
                .set(uuid)
                .executeQuery();

            if (!query.success) return null;
            return new Member(uuid, query.getString("faction"));
        }

        public static Member add(UUID uuid, String faction) {
            Query query = new Query("INSERT INTO Member VALUES (?, ?);")
                .set(uuid, faction)
                .executeUpdate();

            if (!query.success) return null;
            return new Member(uuid, faction);
        }
        public static void remove(UUID uuid) {
            new Query("DELETE FROM Member WHERE uuid = ?;")
                .set(uuid)
                .executeUpdate();
        }
    }

    public class Claims {
        public static Claim get(int x, int z, String level) {
            Query query = new Query("SELECT faction FROM Claim WHERE x = ? AND z = ? AND level = ?;")
                .set(x, z, level)
                .executeQuery();

            if (!query.success) return null;
            return new Claim(x, z, level, query.getString("faction"));
        }

        public static Claim add(int x, int z, String level, String faction) {
            Query query = new Query("INSERT INTO Claim VALUES (?, ?, ?, ?);")
                .set(x, z, level, faction)
                .executeUpdate();

            if (!query.success) return null;
            return new Claim(x, z, level, faction);
        }

        public static void remove(int x, int z, String level) {
            new Query("DELETE FROM Claim WHERE x = ? AND z = ? AND level = ?;")
                .set(x, z, level)
                .executeUpdate();
        }
    }
}
