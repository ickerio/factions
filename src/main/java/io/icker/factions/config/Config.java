package io.icker.factions.config;

import java.util.ArrayList;

import com.google.gson.JsonObject;

import io.icker.factions.config.Zone.Type;

public class Config {
    public static enum HomeOptions {
        ANYWHERE,
        CLAIMS,
        DISABLED;
    }

    public static ArrayList<Zone> ZONES = new ArrayList<Zone>();
    public static int BASE_POWER;
    public static int MEMBER_POWER;
    public static int CLAIM_WEIGHT;
    public static int MAX_FACTION_SIZE;
    public static int SAFE_TICKS_TO_WARP;
    public static int POWER_DEATH_PENALTY;
    public static int TICKS_FOR_POWER;
    public static int TICKS_FOR_POWER_REWARD;
    public static int REQUIRED_BYPASS_LEVEL;
    public static HomeOptions HOME;

    public static void init() {
        JsonObject obj = Parser.load();
        
        Parser.asArray(obj, "zones").forEach(e -> {
            if (!e.isJsonObject()) return;
            JsonObject zoneObj = e.getAsJsonObject();
            
            Type type = Parser.asEnum(zoneObj, "type", Type.class, Type.DEFAULT);
            String message = Parser.asString(zoneObj, "message", "No fail message set");

            Zone zone = new Zone(type, message);
            zone.x = Parser.asConstraint(zoneObj, "x");
            zone.z = Parser.asConstraint(zoneObj, "z");

            JsonObject dimensions = Parser.asObject(zoneObj, "dimensions");
            zone.includedDimensions = Parser.asDimensionList(dimensions, "include");
            zone.excludedDimensions = Parser.asDimensionList(dimensions, "exclude");

           ZONES.add(zone);
        });

        BASE_POWER = Parser.asInt(obj, "basePower", 20);
        MEMBER_POWER = Parser.asInt(obj, "memberPower", 20);
        CLAIM_WEIGHT = Parser.asInt(obj, "claimWeight", 5);
        MAX_FACTION_SIZE = Parser.asInt(obj, "maxFactionSize", -1);
        SAFE_TICKS_TO_WARP = Parser.asInt(obj, "safeTicksToWarp", 5 * 20);
        POWER_DEATH_PENALTY = Parser.asInt(obj, "powerDeathPenalty", 10);
        TICKS_FOR_POWER = Parser.asInt(obj, "ticksForPower", 10 * 60 * 20);
        TICKS_FOR_POWER_REWARD = Parser.asInt(obj, "ticksForPowerReward", 1);
        REQUIRED_BYPASS_LEVEL = Parser.asInt(obj, "requiredBypassLevel", 2);
        HOME = Parser.asEnum(obj, "home", HomeOptions.class, HomeOptions.CLAIMS);
    }

    public static Zone getZone(String dimension, int x, int z) {
        return ZONES.stream().filter(zone -> zone.isApplicable(dimension, x, z)).findFirst().orElse(new Zone(Type.DEFAULT, "No Zones Set"));
    };
}
