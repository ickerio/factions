package io.icker.factions.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import io.icker.factions.FactionsMod;
import io.icker.factions.config.zones.Zone;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
    private static final int REQUIRED_VERSION = 2;
    private static final File file = FabricLoader.getInstance().getGameDir().resolve("config").resolve("factions.json").toFile();

    public static Config load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create(); 

        try {
            if (!file.exists()) {
                file.getParentFile().mkdir();

                Config defaults = new Config();

                FileWriter writer = new FileWriter(file);
                gson.toJson(defaults, writer);
                writer.close();

                return defaults;
            }
    
            Config config = gson.fromJson(new FileReader(file), Config.class);
    
            if (config.VERSION != REQUIRED_VERSION) {
                FactionsMod.LOGGER.error(String.format("Config file incompatible (requires version %d)", REQUIRED_VERSION));
            }

            return config;
        } catch (Exception e) {
            FactionsMod.LOGGER.error("An error occurred reading the factions config file", e);
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

    @SerializedName("radarEnabled")
    public boolean RADAR = true;

    @SerializedName("friendlyFireEnabled")
    public boolean FRIENDLY_FIRE = false;

    @SerializedName("zones")
    public Zone[] ZONES = {};
}
