package io.icker.factions.teams;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import io.icker.factions.Factions;
import net.minecraft.util.Formatting;

public class Database {
    public static Connection con;

    public static void connect() {
        try {
            con = DriverManager.getConnection("jdbc:h2:./factions/factions");
            Statement stmt = con.createStatement();
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Team (
                    name VARCHAR(255) PRIMARY KEY,
                    description VARCHAR(255),
                    color VARCHAR(255),
                    power INTEGER
                );

                CREATE TABLE IF NOT EXISTS Member (
                    uuid UUID PRIMARY KEY,
                    team VARCHAR(255),
                    FOREIGN KEY(team) REFERENCES Team(name)
                );

                CREATE TABLE IF NOT EXISTS Claim (
                    x INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    level VARCHAR(255),
                    team VARCHAR(255),
                    FOREIGN KEY(team) REFERENCES Team(name)
                );""");
            stmt.close();
            Factions.LOGGER.info("Successfully connected to database");
        } catch (SQLException e) {
            e.printStackTrace();    
            Factions.LOGGER.error("Error connecting to and setting up database");
        }
    }

    public static void disconnect() {
        try {
            con.close();
            Factions.LOGGER.info("Successfully disconnected from database");
        } catch (SQLException e) {
            Factions.LOGGER.error("Error disconnecting from database");
        }
    }

    // TODO: stmt.close() on gets
    public class Teams {
        public static Team get(String name) {
            try {
                PreparedStatement stmt = con.prepareStatement("SELECT description, color, power FROM Team WHERE name = ?;");
                stmt.setString(1, name);
    
                ResultSet rs = stmt.executeQuery();
                rs.next();
    
                String description = rs.getString("description");
                Formatting color = Formatting.byName(rs.getString("color"));
                int power = rs.getInt("power");
                return new Team(name, description, color, power);
            } catch (SQLException e) {
                Factions.LOGGER.error("Could not get Team with name {}", name);
                return null;
            }
        }

        public static Team add(String name, String description, String color, int power) {
            try {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO Team VALUES (?, ?, ?, ?);");
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setString(3, color);
                stmt.setInt(4, power);
                
                int affectedRows = stmt.executeUpdate();
                stmt.close();
                if (affectedRows == 0) throw new SQLException("Creating team failed, no rows affected.");

                return new Team(name, description, Formatting.byName(color), power);
            } catch (SQLException e) {
                Factions.LOGGER.error("Could not add Team with name {}", name);
                return null;
            }
        }
    }

    public class Members {
        public static Member get(UUID uuid) {
            try {
                PreparedStatement stmt = con.prepareStatement("SELECT team FROM Member WHERE uuid = ?;");
                stmt.setObject(1, uuid);
        
                ResultSet rs = stmt.executeQuery();
                rs.next();
        
                String team = rs.getString("team");
                return new Member(uuid, team);
            } catch (SQLException e) {
                Factions.LOGGER.error("Could not get Member with uuid {}", uuid); // returns error on claim cos needs to check if exists
                return null;
            }
        }
    
        public static Member add(UUID uuid, String team) {
            try {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO Member VALUES (?, ?);");
                stmt.setObject(1, uuid);
                stmt.setString(2, team);
            
                int affectedRows = stmt.executeUpdate();
                stmt.close();
                if (affectedRows == 0) throw new SQLException("Creating member failed, no rows affected.");
                return new Member(uuid, team);
            } catch (SQLException e) {
                Factions.LOGGER.error("Could not add Member with uuid {}", uuid);
                return null;
            }
        }
    }

    public class Claims {
        public static Claim get(int x, int z, String level) {
            try {
                PreparedStatement stmt = con.prepareStatement("SELECT team FROM Claim WHERE x = ? AND z = ? AND level = ?;");
                stmt.setInt(1, x);
                stmt.setInt(2, z);
                stmt.setString(3, level);
        
                ResultSet rs = stmt.executeQuery();
                rs.next();
        
                String team = rs.getString("team");
                return new Claim(x, z, level, team);
            } catch (SQLException e) {
                Factions.LOGGER.error("Could not get Claim with x {}, z {} and level {}", x, z, level);
                return null;
            }
        }
    
        public static ArrayList<Claim> getMultiple(String teamName) {
            try {
                PreparedStatement stmt = con.prepareStatement("SELECT x, z, level FROM Claim WHERE team = ?;");
                stmt.setString(1, teamName);
        
                ArrayList<Claim> claims = new ArrayList<Claim>();
        
                ResultSet rs = stmt.executeQuery();
                while(rs.next()) { 
                    int x  = rs.getInt("x");
                    int z  = rs.getInt("z"); 
                    String level  = rs.getString("level"); 
                    
                    claims.add(new Claim(x, z, level, teamName));
                }
                return claims;
            } catch (SQLException e) {
                Factions.LOGGER.error("Could not get Claims with team name {}", teamName);
                return null;
            }
        }
    
        public static Claim add(int x, int z, String level, String team) {
            try {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO Claim VALUES (?, ?, ?, ?);");
                stmt.setInt(1, x);
                stmt.setInt(2, z);
                stmt.setString(3, level);
                stmt.setString(4, team);
        
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Creating claim failed, no rows affected.");
                return new Claim(x, z, level, team);
            } catch (SQLException e) {
                Factions.LOGGER.error("Could not add Claim with x {}, z {} and level {}", x, z, level);
                return null;
            }
        }

        public static void remove(int x, int z, String level, String team) {
            try {
                PreparedStatement stmt = con.prepareStatement("DELETE FROM Claim WHERE x = ? AND z = ? AND level = ?;");
                stmt.setInt(1, x);
                stmt.setInt(2, z);
                stmt.setString(3, level);
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                Factions.LOGGER.error("Could not remove Claim with x {}, z {} and level {}", x, z, level);
            }
        }
    }
}
