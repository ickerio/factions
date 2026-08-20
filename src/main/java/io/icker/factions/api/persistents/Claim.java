package io.icker.factions.api.persistents;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.util.WorldUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Name("Claim")
public class Claim {
    public record ClaimKey(int x, int z, String level) {}

    private static final HashMap<ClaimKey, Claim> STORE = Database.load(Claim.class, Claim::getKey);
    private static final FactionClaimCounts COUNTS_BY_FACTION = buildFactionCounts();

    @Field("X")
    public int x;

    @Field("Z")
    public int z;

    /** The dimension of the claim */
    @Field("Level")
    public String level;

    @Field("FactionID")
    public UUID factionID;

    @Field("AccessLevel")
    public Rank accessLevel;

    public Claim(int x, int z, String level, UUID factionID) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.factionID = factionID;
        this.accessLevel = Rank.MEMBER;
    }

    public Claim() {}

    public ClaimKey getKey() {
        return new ClaimKey(x, z, level);
    }

    public static Claim get(int x, int z, String level) {
        return STORE.get(new ClaimKey(x, z, level));
    }

    public static Claim get(Level world, BlockPos pos) {
        return get(pos.getX() >> 4, pos.getZ() >> 4, WorldUtils.dimensionString(world));
    }

    public static List<Claim> getByFaction(UUID factionID) {
        return STORE.values().stream().filter(c -> c.factionID.equals(factionID)).toList();
    }

    public static int getCountByFaction(UUID factionID) {
        return COUNTS_BY_FACTION.get(factionID);
    }

    public static void audit() {
        STORE.values().removeIf(Claim::isInvalid);
        rebuildFactionCounts();
    }

    public static void add(Claim claim) {
        Claim replaced = STORE.put(claim.getKey(), claim);
        COUNTS_BY_FACTION.replace(replaced == null ? null : replaced.factionID, claim.factionID);
        ClaimEvents.ADD.invoker().onAdd(claim);
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }

    public void remove() {
        Claim removed = STORE.remove(getKey());
        if (removed == null) return;

        COUNTS_BY_FACTION.remove(removed.factionID);
        ClaimEvents.REMOVE
                .invoker()
                .onRemove(
                        removed.x,
                        removed.z,
                        removed.level,
                        Faction.get(removed.factionID));
    }

    public static void save() {
        Database.save(Claim.class, STORE.values().stream().toList());
    }

    private static FactionClaimCounts buildFactionCounts() {
        FactionClaimCounts counts = new FactionClaimCounts();
        for (Claim claim : STORE.values()) {
            counts.add(claim.factionID);
        }
        return counts;
    }

    private static void rebuildFactionCounts() {
        COUNTS_BY_FACTION.clear();
        for (Claim claim : STORE.values()) {
            COUNTS_BY_FACTION.add(claim.factionID);
        }
    }

    private static boolean isInvalid(Claim claim) {
        return Faction.get(claim.factionID) == null || !WorldUtils.isValid(claim.level);
    }
}
