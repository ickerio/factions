package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.UUID;

import io.icker.factions.api.events.HomeEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

@Name("Home")
public class Home implements Persistent {
    private static final HashMap<UUID, Home> STORE = Database.load(Home.class, h -> h.factionID);

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
        return factionID.toString();
    }

    public static Home get(UUID factionID) {
        return STORE.get(factionID);
    }

    public static void set(Home home) {
        STORE.put(home.factionID, home);
        HomeEvents.SET.invoker().onSet(home);
    }

    public static void save() {
        Database.save(Home.class, STORE.values().stream().toList());
    }
}
