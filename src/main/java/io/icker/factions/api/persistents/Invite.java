package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

public class Invite implements Persistent {
    @Field("PlayerID")
    private UUID playerID;

    @Field("FactionID")
    private UUID factionID;

    public Invite(UUID playerID, UUID factionID) {
        this.playerID = playerID;
        this.factionID = factionID;
    }

    public Invite() { ; }

    public String getKey() {
        return playerID.toString() + "-" + factionID.toString();
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }
}
