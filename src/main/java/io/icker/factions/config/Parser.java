package io.icker.factions.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.icker.factions.FactionsMod;
import net.fabricmc.loader.api.FabricLoader;

public class Parser {
    private static final File factionDir = FabricLoader.getInstance().getGameDir().resolve("factions").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int version = 1;

    public static JsonObject obj;

    public static void load() {
        File config = new File(factionDir, "config.json");

        if (config.exists()) {
            try {
                FileReader reader = new FileReader(config);
                obj = GSON.fromJson(reader, JsonObject.class);

                if (asInt("version", 0) != version) {
                    FactionsMod.LOGGER.warn(String.format("Factions config file incompatible or version not specified (Requires version %d)", version));
                }
                reader.close();
            } catch (IOException e) {
                FactionsMod.LOGGER.error("Factions config file failed to load");
            }
        } else {
            FactionsMod.LOGGER.warn("Factions config file not present, using default values");
            obj = new JsonObject();
        }
    }

    public static int asInt(String key, int fallback) {
        try {
            return obj.get(key).getAsInt();
        } catch (NullPointerException | UnsupportedOperationException e) {
            return fallback;
        }
    }

    public static String asString(String key, String fallback) {
        try {
            return obj.get(key).getAsString();
        } catch (NullPointerException | UnsupportedOperationException e) {
            return fallback;
        }
    }

    public static <T extends Enum<T>> T asEnum(String key, Class<T> c, T fallback) {
        try {
            return Enum.valueOf(c, obj.get(key).getAsString().trim().toUpperCase());
        } catch (NullPointerException | UnsupportedOperationException | IllegalArgumentException e) {
            return fallback;
        }
    }

    public static JsonArray asArray(String key) {
        try {
            return obj.get(key).getAsJsonArray();
        } catch (NullPointerException | UnsupportedOperationException e) {
            return new JsonArray();
        }
    }
}
