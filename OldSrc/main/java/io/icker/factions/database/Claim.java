package io.icker.factions.database;

public class Claim {
    public int x;
    public int z;
    public String level;
    private String factionName;

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

    public Claim(int x, int z, String level, String faction) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.factionName = faction;
    }

    public Faction getFaction() {
        return Faction.get(factionName);
    }

    public void remove() {
        new Query("DELETE FROM Claim WHERE x = ? AND z = ? AND level = ?;")
            .set(x, z, level)
            .executeUpdate();
    }
}
