package io.icker.factions.api.persistents;

import io.icker.factions.database.Field;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class Home {
    @Field("X")
    public double x;

    @Field("Y")
    public double y;

    @Field("Z")
    public double z;

    @Field("Yaw")
    public float yaw;

    @Field("Pitch")
    public float pitch;

    @Field("Level")
    public String level;

    private UUID factionID;

    @Field("Home Cooldown")
    public HashMap<ServerPlayerEntity, Long> HomeCooldown = new HashMap<>();

    public Home(
            UUID factionID, double x, double y, double z, float yaw, float pitch, String level) {
        this.factionID = factionID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.level = level;

    }

    public Home() {}

    public void setPlayerLastWarpTime(ServerPlayerEntity player)
    {
        HomeCooldown.put(player, Date.from(Instant.now()).getTime());
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }
}
