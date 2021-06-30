package io.icker.factions.config;

// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;

public class Config {
    public static int BASE_POWER;
    public static int MEMBER_POWER;
    public static int CLAIM_WEIGHT;
    public static int MAX_FACTION_SIZE;
    public static int SAFE_TICKS_TO_WARP;
    public static int POWER_DEATH_PENALTY;
    public static int TICKS_FOR_POWER;
    public static int TICKS_FOR_POWER_REWARD;
    public static String HOME_COMMAND;

    public static void init() {
        ConfigParser.load();
                
        BASE_POWER = ConfigParser.asInt("basePower", 20);
        MEMBER_POWER = ConfigParser.asInt("memberPower", 20);
        CLAIM_WEIGHT = ConfigParser.asInt("claimWeight", 10);
        MAX_FACTION_SIZE = ConfigParser.asInt("maxFactionSize", -1);
        SAFE_TICKS_TO_WARP = ConfigParser.asInt("safeTicksToWarp", 5 * 20);
        POWER_DEATH_PENALTY = ConfigParser.asInt("powerDeathPenalty", -10);
        TICKS_FOR_POWER = ConfigParser.asInt("ticksForPower", 10 * 60 * 20);
        TICKS_FOR_POWER_REWARD = ConfigParser.asInt("ticksForPowerReward", 1);
    }
}
