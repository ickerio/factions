package io.icker.factions.database;

import java.util.UUID;

public class Member {
    public UUID uuid;
    private String factionName;

    public Member(UUID uuid, String faction) {
        this.uuid = uuid;
        this.factionName = faction;
    }

    public Faction getFaction() {
        return Database.Factions.get(factionName);
    }
}