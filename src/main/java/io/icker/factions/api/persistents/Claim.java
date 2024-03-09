package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import net.minecraft.util.math.BlockPos;

@Name("Claim")
public class Claim {
    private static final HashMap<String, Claim> STORE = Database.load(Claim.class, c -> c.getKey());

    @Field("X")
    public int x;

    @Field("Z")
    public int z;

    @Field("level")
    public String level;

    @Field("factionID")
    public UUID factionID;

    @Field("create")
    public boolean create;

    @Field("outpost")
    public Outpost outpost;

    public Claim(int x, int z, String level, UUID factionID, boolean create, Outpost outpost) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.factionID = factionID;
        this.create = create;
        this.outpost = outpost;
    }

    public Claim() { ; }

    public String getKey() {
        return String.format("%s-%d-%d", level, x, z);
    }

    public static Claim get(int x, int z, String level) {
        return STORE.get(String.format("%s-%d-%d", level, x, z));
    }

    public static List<Claim> getByFaction(UUID factionID) {
        return STORE.values()
            .stream()
            .filter(c -> c.factionID.equals(factionID))
            .toList();
    }

    public static void add(Claim claim) {
        STORE.put(claim.getKey(), claim);
        ClaimEvents.ADD.invoker().onAdd(claim);
    }

    public boolean isOutpost(){
        return this.outpost != null;
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }

    public void remove() {
        STORE.remove(getKey());
        ClaimEvents.REMOVE.invoker().onRemove(x, z, level, Faction.get(factionID));
    }

    public static void save() {
        Database.save(Claim.class, STORE.values().stream().toList());
    }

    public static class Outpost {
        @Field("homePos")
        public BlockPos homePos;

        @Field("x")
        public int x;
        @Field("z")
        public int z;
        @Field("index")
        public int index;

        @Field("level")
        public String level;
        public Outpost(int x, int z, BlockPos homePos, int index, String level){
            this.x = x; this.z = z;
            this.homePos = homePos;
            this.index = index;
            this.level = level;
        }
    }
}
