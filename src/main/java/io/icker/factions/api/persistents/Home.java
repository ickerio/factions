package io.icker.factions.api.persistents;

import java.util.UUID;

import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

public class Home implements Persistent {
    @Field("FactionID")
    public UUID factionID;

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

    public Home(UUID factionID, double x, double y, double z, float yaw, float pitch, String level) {
        this.factionID = factionID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.level = level;
    }

    public Home() { ; }

    public String getKey() {
        return "home";
    }
    
    public Faction getFaction() {
        return Faction.get(factionID);
    }
}
