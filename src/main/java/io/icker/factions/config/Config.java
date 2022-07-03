package io.icker.factions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import io.icker.factions.FactionsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

public class Config {
    private static final int REQUIRED_VERSION = 3;
    private static final File file = FabricLoader.getInstance().getGameDir().resolve("config").resolve("factions.json").toFile();

    public static Config load() {
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .registerTypeAdapter(HomeConfig.class, new HomeConfig.Deserializer())
            .registerTypeAdapter(PowerConfig.class, new PowerConfig.Deserializer())
            .registerTypeAdapter(SafeConfig.class, new SafeConfig.Deserializer())
            .create();

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

    @SerializedName("version")
    public int VERSION = REQUIRED_VERSION;

    @SerializedName("power")
    public PowerConfig POWER = new PowerConfig();

    @SerializedName("safe")
    public SafeConfig SAFE = new SafeConfig();

    @SerializedName("home")
    public HomeConfig HOME = new HomeConfig();

    @SerializedName("display")
    public DisplayConfig DISPLAY = new DisplayConfig();

    @SerializedName("maxFactionSize")
    public int MAX_FACTION_SIZE = -1;

    @SerializedName("friendlyFire")
    public boolean FRIENDLY_FIRE = false;

    @SerializedName("requiredBypassLevel")
    public int REQUIRED_BYPASS_LEVEL = 2;

    @SerializedName("nameBlackList")
    public List<String> NAME_BLACKLIST = List.of("wilderness", "factionless");

    public static class DisplayConfig {
        @SerializedName("factionNameMaxLength")
        public int NAME_MAX_LENGTH = -1;

        @SerializedName("changeChat")
        public boolean MODIFY_CHAT = true;

        @SerializedName("tabMenu")
        public boolean TAB_MENU = true;
    }
}
