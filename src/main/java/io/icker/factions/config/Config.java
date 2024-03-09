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
    private static final int REQUIRED_VERSION = 3;
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

    public enum SafeOptions {
        @SerializedName("ENABLED")
        ENABLED,

        @SerializedName("ENDERCHEST")
        ENDERCHEST,

        @SerializedName("COMMAND")
        COMMAND,

        @SerializedName("DISABLED")
        DISABLED,
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
    public int POWER_DEATH_PENALTY = 0;

    @SerializedName("ticksForPower")
    public int TICKS_FOR_POWER = -1;

    @SerializedName("ticksForPowerReward")
    public int TICKS_FOR_POWER_REWARD = 0;

    @SerializedName("requiredBypassLevel")
    public int REQUIRED_BYPASS_LEVEL = 2;

    @SerializedName("home")
    public HomeOptions HOME = HomeOptions.CLAIMS;

    @SerializedName("radarEnabled")
    public boolean RADAR = true;

    @SerializedName("friendlyFireEnabled")
    public boolean FRIENDLY_FIRE = false;

    @SerializedName("chatModificationEnabled")
    public boolean MODIFY_CHAT = true;

    @SerializedName("factionSafe")
    public SafeOptions FACTION_SAFE = SafeOptions.ENABLED;

    @SerializedName("factionSafeDouble")
    public boolean FACTION_SAFE_DOUBLE = true;

    @SerializedName("diamondCurrency")
    public int DIAMOND_CURRENCY = 16;

    @SerializedName("dailyTaxPerChunk")
    public int DAILY_TAX_PER_CHUNK = 1;

    @SerializedName("maxPower")
    public int MAX_POWER = 8192;

    @SerializedName("offwarHoursStart")
    public int OFFWAR_HOURS_START = 23;

    @SerializedName("offwarHoursEnd")
    public int OFFWAR_HOURS_END = 10;

    @SerializedName("warTaxes")
    public int DECLARE_WAR_TAXES = 256;

    @SerializedName("FabricateTaxes")
    public int FABRICATE_TAXES = 8;


    @SerializedName("taxesHours")
    public int TAXES_HOURS = 12;


    @SerializedName("taxesMinutes")
    public int TAXES_MINUTES = 0;

    @SerializedName("spawnRadius")
    public int SPAWN_RADIUS = 512;

    @SerializedName("outpostCost")
    public int OUTPOST_COST = 256;

    @SerializedName("daysToFabricate")
    public int DAYS_TO_FABRICATE = 3;

    @SerializedName("hoursBeforeNextFabricate")
    public long HOURS_BEFORE_NEXT_FABRICATE = 12;
}
