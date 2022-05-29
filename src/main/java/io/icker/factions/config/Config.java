package io.icker.factions.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import io.icker.factions.FactionsMod;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
    private static final int REQUIRED_VERSION = 1;
    private static final File file = FabricLoader.getInstance().getGameDir().resolve("factions").resolve("config.json").toFile();

    public static Config load() {
        Gson gson = new GsonBuilder().create(); 

        try {
            if (!file.exists()) {
                Config defaults = new Config();
                gson.toJson(defaults, new FileWriter(file));
                return defaults;
            }
    
            Config config = gson.fromJson(new FileReader(file), Config.class);
    
            if (config.VERSION != REQUIRED_VERSION) {
                FactionsMod.LOGGER.error(String.format("Config file incompatible (requires version %d)", REQUIRED_VERSION));
            }

            return config;
        } catch (Exception e) {
            FactionsMod.LOGGER.error("An error occurred reading the factions config file");
            return new Config();
        }
    }

    public enum HomeOptions {
        @SerializedName("ANYWHERE")
        ANYWHERE,

        @SerializedName("CLAIMS")
        CLAIMS,

        @SerializedName("DISABLED")
        DISABLED
    }

    @SerializedName("version")
    public int VERSION = REQUIRED_VERSION;

    @SerializedName("basePower")
    public int BASE_POWER = 20;

    @SerializedName("memberPower")
    public int MEMBER_POWER = 20;
    
    @SerializedName("claimWeight")
    public int CLAIM_WEIGHT = 5;

    @SerializedName("maxFactionSize")
    public int MAX_FACTION_SIZE = -1;
    
    @SerializedName("safeTicksToWarp")
    public int SAFE_TICKS_TO_WARP = 1000;

    @SerializedName("powerDeathPenalty")
    public int POWER_DEATH_PENALTY = 10;

    @SerializedName("ticksForPower")
    public int TICKS_FOR_POWER = 12000;

    @SerializedName("ticksForPowerReward")
    public int TICKS_FOR_POWER_REWARD = 1;

    @SerializedName("requiredBypassLevel")
    public int REQUIRED_BYPASS_LEVEL = 2;

    @SerializedName("home")
    public HomeOptions HOME = HomeOptions.CLAIMS;

    @SerializedName("zoneMessageEnabled")
    public boolean ZONE_MESSAGE = true;

    @SerializedName("friendlyFireEnabled")
    public boolean FRIENDLY_FIRE = false;
}
