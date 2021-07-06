package io.icker.factions.database;

public class Home {
    public String factionName;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public String level;

    public static Home get(String factionName) {
        Query query = new Query("SELECT * FROM Home WHERE faction = ?;")
            .set(factionName)
            .executeQuery();

        if (!query.success) return null;
        return new Home(factionName,
            query.getDouble("x"), query.getDouble("y"),query.getDouble("z"),
            query.getFloat("yaw"), query.getFloat("pitch"), query.getString("level")
        );
    }

    public static Home set(String factionName, double x, double y, double z, float yaw, float pitch, String level) {
        Query query = new Query("MERGE INTO Home KEY (faction) VALUES (?, ?, ?, ?, ?, ?, ?);")
            .set(factionName, x, y, z, yaw, pitch, level)
            .executeUpdate();

        if (!query.success) return null;
        return new Home(factionName, x, y, z, yaw, pitch, level);
    }
    
    public Home(String faction, double x, double y, double z, float yaw, float pitch, String level) {
        this.factionName = faction;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.level = level;
    }
}
