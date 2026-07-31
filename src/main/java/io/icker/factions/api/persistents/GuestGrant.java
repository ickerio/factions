package io.icker.factions.api.persistents;

import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Name("GuestGrant")
public class GuestGrant {
    private static final HashMap<String, GuestGrant> STORE =
            Database.load(GuestGrant.class, GuestGrant::getKey);

    /** Worst-case window of quota loss on an unclean shutdown. Lower = safer, more writes. */
    private static final long FLUSH_INTERVAL_MILLIS = 30_000L;

    private static boolean dirty;
    private static long lastFlushMillis;

    @Field("ID")
    private UUID id;

    @Field("FactionID")
    public UUID factionID;

    @Field("PlayerID")
    public UUID playerID;

    @Field("BreakRemaining")
    public int breakRemaining;

    @Field("PlaceRemaining")
    public int placeRemaining;

    public GuestGrant(UUID factionID, UUID playerID, int breakRemaining, int placeRemaining) {
        this.id = UUID.randomUUID();
        this.factionID = factionID;
        this.playerID = playerID;
        this.breakRemaining = breakRemaining;
        this.placeRemaining = placeRemaining;
    }

    public GuestGrant() {}

    public String getKey() {
        return key(factionID, playerID);
    }

    public static String key(UUID factionID, UUID playerID) {
        return factionID + "-" + playerID;
    }

    public static GuestGrant get(UUID factionID, UUID playerID) {
        return STORE.get(key(factionID, playerID));
    }

    public static List<GuestGrant> getByFaction(UUID factionID) {
        return STORE.values().stream().filter(g -> g.factionID.equals(factionID)).toList();
    }

    public static void add(GuestGrant grant) {
        STORE.put(grant.getKey(), grant);
        dirty = true;
    }

    public void consumeBreak() {
        breakRemaining--;
        dirty = true;
    }

    public void consumePlace() {
        placeRemaining--;
        dirty = true;
    }

    public void remove() {
        if (STORE.remove(getKey()) != null) {
            dirty = true;
        }
    }

    public static void save() {
        if (!dirty) return;

        Database.save(GuestGrant.class, STORE.values().stream().toList());
        dirty = false;
        lastFlushMillis = System.currentTimeMillis();
    }

    public static void saveThrottled() {
        if (!dirty) return;
        if (System.currentTimeMillis() - lastFlushMillis < FLUSH_INTERVAL_MILLIS) return;

        save();
    }
}
