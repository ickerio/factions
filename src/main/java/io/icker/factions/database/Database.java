package io.icker.factions.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import io.icker.factions.FactionsMod;
import net.minecraft.util.Formatting;

public class Database {
    public static Connection con;

    public static void connect() {
        try {
            con = DriverManager.getConnection("jdbc:h2:./factions/factions");
            Statement stmt = con.createStatement();
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Faction (
                    name VARCHAR(255) PRIMARY KEY,
                    description VARCHAR(255),
                    color VARCHAR(255),
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
                );""");
            stmt.close();
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
            try {
                PreparedStatement stmt = con.prepareStatement("SELECT description, color, power FROM Faction WHERE name = ?;");
                stmt.setString(1, name);
    
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) return null;
    
                String description = rs.getString("description");
                Formatting color = Formatting.byName(rs.getString("color"));
                int power = rs.getInt("power");
                return new Faction(name, description, color, power);
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not get Faction with name {}", name);
                return null;
            }
        }

        public static Faction add(String name, String description, String color, int power) {
            try {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO Faction VALUES (?, ?, ?, ?);");
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setString(3, color);
                stmt.setInt(4, power);
                
                int affectedRows = stmt.executeUpdate();
                stmt.close();
                if (affectedRows == 0) throw new SQLException("Creating Faction failed, no rows affected.");

                return new Faction(name, description, Formatting.byName(color), power);
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not add Faction with name {}", name);
                return null;
            }
        }

        public static void remove(String name) {
            try {
                PreparedStatement stmt = con.prepareStatement("DELETE FROM Faction WHERE name = ?;");
                stmt.setString(1, name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not remove Faction with name {}", name);
            }
        }

    }

    public class Members {
        public static Member get(UUID uuid) {
            try {
                PreparedStatement stmt = con.prepareStatement("SELECT faction FROM Member WHERE uuid = ?;");
                stmt.setObject(1, uuid);
        
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) return null;
        
                String faction = rs.getString("faction");
                return new Member(uuid, faction);
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not get Member with uuid {}", uuid);
                return null;
            }
        }
    
        public static Member add(UUID uuid, String faction) {
            try {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO Member VALUES (?, ?);");
                stmt.setObject(1, uuid);
                stmt.setString(2, faction);
            
                int affectedRows = stmt.executeUpdate();
                stmt.close();
                if (affectedRows == 0) throw new SQLException("Creating member failed, no rows affected.");
                return new Member(uuid, faction);
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not add Member with uuid {}", uuid);
                return null;
            }
        }
    }

    public class Claims {
        public static Claim get(int x, int z, String level) {
            try {
                PreparedStatement stmt = con.prepareStatement("SELECT faction FROM Claim WHERE x = ? AND z = ? AND level = ?;");
                stmt.setInt(1, x);
                stmt.setInt(2, z);
                stmt.setString(3, level);
        
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) return null;
        
                String faction = rs.getString("faction");
                return new Claim(x, z, level, faction);
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not get Claim with x {}, z {} and level {}", x, z, level);
                return null;
            }
        }
    
        public static ArrayList<Claim> getMultiple(String factionName) {
            try {
                PreparedStatement stmt = con.prepareStatement("SELECT x, z, level FROM Claim WHERE faction = ?;");
                stmt.setString(1, factionName);
        
                ArrayList<Claim> claims = new ArrayList<Claim>();
        
                ResultSet rs = stmt.executeQuery();
                while(rs.next()) { 
                    int x  = rs.getInt("x");
                    int z  = rs.getInt("z"); 
                    String level  = rs.getString("level"); 
                    
                    claims.add(new Claim(x, z, level, factionName));
                }
                return claims;
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not get Claims with faction name {}", factionName);
                return null;
            }
        }
    
        public static Claim add(int x, int z, String level, String faction) {
            try {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO Claim VALUES (?, ?, ?, ?);");
                stmt.setInt(1, x);
                stmt.setInt(2, z);
                stmt.setString(3, level);
                stmt.setString(4, faction);
        
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Creating claim failed, no rows affected.");
                return new Claim(x, z, level, faction);
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not add Claim with x {}, z {} and level {}", x, z, level);
                return null;
            }
        }

        public static void remove(int x, int z, String level) {
            try {
                PreparedStatement stmt = con.prepareStatement("DELETE FROM Claim WHERE x = ? AND z = ? AND level = ?;");
                stmt.setInt(1, x);
                stmt.setInt(2, z);
                stmt.setString(3, level);
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                FactionsMod.LOGGER.error("Could not remove Claim with x {}, z {} and level {}", x, z, level);
            }
        }
    }
}
