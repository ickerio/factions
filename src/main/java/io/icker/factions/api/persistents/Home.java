package io.icker.factions.api.persistents;

import java.util.UUID;
import io.icker.factions.database.Field;

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

    public Home(UUID factionID, double x, double y, double z, float yaw, float pitch,
            String level) {
        this.factionID = factionID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.level = level;
    }

    @SuppressWarnings("unused")
    public Home() {}

    public Faction getFaction() {
        return Faction.get(factionID);
    }
}
