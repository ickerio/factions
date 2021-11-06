package io.icker.factions.database;

import java.util.UUID;

public class Member {
    public UUID uuid;
    private String factionName;

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

    public Member(UUID uuid, String faction) {
        this.uuid = uuid;
        this.factionName = faction;
    }

    public Faction getFaction() {
        return Faction.get(factionName);
    }

    public void remove() {
        new Query("DELETE FROM Member WHERE uuid = ?;")
            .set(uuid)
            .executeUpdate();
    }
}