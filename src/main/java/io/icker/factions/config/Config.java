package io.icker.factions.config;

public class Config {
    public static enum HomeOptions {
        ANYWHERE,
        CLAIMS,
        DISABLED;
    }

    public static int BASE_POWER;
    public static int MEMBER_POWER;
    public static int CLAIM_WEIGHT;
    public static int MAX_FACTION_SIZE;
    public static int SAFE_TICKS_TO_WARP;
    public static int POWER_DEATH_PENALTY;
    public static int TICKS_FOR_POWER;
    public static int TICKS_FOR_POWER_REWARD;
    public static HomeOptions HOME;

    public static void init() {
        Parser.load();
                
        BASE_POWER = Parser.asInt("basePower", 20);
        MEMBER_POWER = Parser.asInt("memberPower", 20);
        CLAIM_WEIGHT = Parser.asInt("claimWeight", 5);
        MAX_FACTION_SIZE = Parser.asInt("maxFactionSize", -1);
        SAFE_TICKS_TO_WARP = Parser.asInt("safeTicksToWarp", 5 * 20);
        POWER_DEATH_PENALTY = Parser.asInt("powerDeathPenalty", -10);
        TICKS_FOR_POWER = Parser.asInt("ticksForPower", 10 * 60 * 20);
        TICKS_FOR_POWER_REWARD = Parser.asInt("ticksForPowerReward", 1);
        HOME = Parser.asEnum("home", HomeOptions.class, HomeOptions.CLAIMS);
    }
}
