package io.icker.factions.config;

// import com.google.gson.JsonArray;
// import com.google.gson.JsonPrimitive;

// import io.icker.factions.config.Zone.Type;

import java.io.IOException;
// import java.util.ArrayList;

public class Config {
    private static final String[] filePaths = {"factions/config.json", "config/factions.json"};
    private static final Integer version = 1;

    public enum HomeOptions {
        ANYWHERE,
        CLAIMS,
        DISABLED
    }

    // public static ArrayList<Zone> ZONES = new ArrayList<Zone>();
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
    public static boolean ZONE_MESSAGE;
    public static boolean FRIENDLY_FIRE;

    // private static final Constraint asConstraint(ObjectParser parser) throws IOException {
    //     Constraint con = new Constraint();

    //     con.equal = parser.getInteger("==", null);
    //     con.notEqual = parser.getInteger("!=", null);
    //     con.lessThan = parser.getInteger("<", null);
    //     con.lessThanOrEqual = parser.getInteger("<=", null);
    //     con.greaterThan = parser.getInteger(">", null);
    //     con.greaterThanOrEqual = parser.getInteger(">=", null);

    //     return con;
    // }

    // private static final ArrayList<String> asDimensionList(JsonArray array) {
    //     ArrayList<String> list = new ArrayList<String>();

    //     array.forEach(e -> {
    //         if (e.isJsonPrimitive()) {
    //             JsonPrimitive primitive = e.getAsJsonPrimitive();
    //             if (primitive.isString()) list.add(primitive.getAsString());
    //         }
    //     });

    //     return list;
    // }

    public static void init() throws IOException {
        FileParser parser = new FileParser(version, filePaths);

        // parser.getArray("zones").forEach(object -> {
        //     try {
        //         if (!object.isJsonObject()) return;
        //         ObjectParser zoneParser = new ObjectParser(object.getAsJsonObject(), parser);

        //         Type type = zoneParser.getEnum("type", Type.class, "DEFAULT");
        //         String message = zoneParser.getString("message", "No fail message set");

        //         Zone zone = new Zone(type, message);
        //         zone.x = asConstraint(zoneParser.getParser("x"));
        //         zone.z = asConstraint(zoneParser.getParser("z"));

        //         ObjectParser dimensions = zoneParser.getParser("dimensions");
        //         zone.includedDimensions = asDimensionList(dimensions.getArray("include"));
        //         zone.excludedDimensions = asDimensionList(dimensions.getArray("exclude"));

        //         ZONES.add(zone);
        //     } catch (IOException e) {}
        // });

        BASE_POWER = parser.getInteger("basePower", 20);
        MEMBER_POWER = parser.getInteger("memberPower", 20);
        CLAIM_WEIGHT = parser.getInteger("claimWeight", 5);
        MAX_FACTION_SIZE = parser.getInteger("maxFactionSize", -1);
        SAFE_TICKS_TO_WARP = parser.getInteger("safeTicksToWarp", 1000);
        POWER_DEATH_PENALTY = parser.getInteger("powerDeathPenalty", 10);
        TICKS_FOR_POWER = parser.getInteger("ticksForPower", 12000);
        TICKS_FOR_POWER_REWARD = parser.getInteger("ticksForPowerReward", 1);
        REQUIRED_BYPASS_LEVEL = parser.getInteger("requiredBypassLevel", 2);
        HOME = parser.getEnum("home", HomeOptions.class, "CLAIMS");
        ZONE_MESSAGE = parser.getBoolean("zoneMessageEnabled", true);
        FRIENDLY_FIRE = parser.getBoolean("friendlyFireEnabled", false);
    }

    // public static Zone getZone(String dimension, int x, int z) {
    //     return ZONES.stream().filter(zone -> zone.isApplicable(dimension, x, z)).findFirst().orElse(new Zone(Type.DEFAULT, "No Zones Set"));
    // };
}
