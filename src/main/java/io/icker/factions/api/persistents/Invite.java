package io.icker.factions.api.persistents;

import java.util.UUID;

import io.icker.factions.database.Field;

public class Invite  {
    @Field("PlayerID")
    private UUID playerID;

    @Field("FactionID")
    private UUID factionID;

    public Invite(UUID playerID, UUID factionID) {
        this.playerID = playerID;
        this.factionID = factionID;
    }

    public Invite() { ; }

    public UUID getPlayerID() {
        return playerID;
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }
}
