package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.icker.factions.api.events.AddClaimEvent;
import io.icker.factions.api.events.RemoveClaimEvent;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

@Name("Claim")
public class Claim implements Persistent {
    private static final HashMap<String, Claim> STORE = new HashMap<String, Claim>();

    @Field("X")
    private int x;

    @Field("Z")
    private int z;

    @Field("Level")
    private String level;

    @Field("FactionID")
    private UUID factionID;

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
