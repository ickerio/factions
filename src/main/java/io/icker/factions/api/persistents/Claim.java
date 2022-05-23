package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.icker.factions.api.events.AddClaimEvent;
import io.icker.factions.api.events.RemoveClaimEvent;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

@Name("Claim")
public class Claim implements Persistent {
    private static final HashMap<String, Claim> STORE = Database.load(Claim.class, c -> c.getKey());

    @Field("X")
    public final int x;

    @Field("Z")
    public final int z;

    @Field("Level")
    public final String level;

    @Field("FactionID")
    public final UUID factionID;

    public Claim(int x, int z, String level, UUID factionID) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.factionID = factionID;
    }

    public String getKey() {
        return String.format("%s/%i-%i", level, x, z);
    }

    public static Claim get(int x, int z, String level) {
        return STORE.get(String.format("%s/%i-%i", level, x, z));
    }

    public static List<Claim> getByFaction(UUID factionID) {
        return STORE.values()
            .stream()
            .filter(c -> c.factionID == factionID)
            .collect(Collectors.toList());
    }

    public static void add(Claim claim) {
        STORE.put(claim.getKey(), claim);
        AddClaimEvent.run(claim);
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }

    public void remove() {
        STORE.remove(getKey());
        RemoveClaimEvent.run(this);
    }
}
