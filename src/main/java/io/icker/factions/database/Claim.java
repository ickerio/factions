package io.icker.factions.database;

import io.icker.factions.api.AddClaimEvent;
import io.icker.factions.api.RemoveClaimEvent;

public class Claim {
    public int x;
    public int z;
    public String level;
    private final String factionName;

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

        AddClaimEvent.run(new Claim(x, z, level, faction));

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

        RemoveClaimEvent.run(this);
    }
}
