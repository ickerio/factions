package io.icker.factions.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.icker.factions.FactionsMod;
import net.fabricmc.loader.api.FabricLoader;

public class Parser {
    private static final File factionDir = FabricLoader.getInstance().getGameDir().resolve("factions").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int version = 1;

    public static JsonObject load() {
        File config = new File(factionDir, "config.json");

        if (config.exists()) {
            try {
                FileReader reader = new FileReader(config);
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);

                if (Parser.asInt(obj, "version", 0) != version) {
                    FactionsMod.LOGGER.warn(String.format("Factions config file incompatible or version not specified (Requires version %d)", version));
                }
                reader.close();

                return obj;
            } catch (IOException e) {
                FactionsMod.LOGGER.error("Factions config file failed to load");
                return new JsonObject();
            }
        } else {
            FactionsMod.LOGGER.warn("Factions config file not present, using default values");
            return new JsonObject();
        }
    }

    public static Integer asInt(JsonObject obj, String key, Integer fallback) {
        try {
            return obj.get(key).getAsInt();
        } catch (NullPointerException | UnsupportedOperationException e) {
            return fallback;
        }
    }

    public static String asString(JsonObject obj, String key, String fallback) {
        try {
            return obj.get(key).getAsString();
        } catch (NullPointerException | UnsupportedOperationException e) {
            return fallback;
        }
    }

    public static <T extends Enum<T>> T asEnum(JsonObject obj, String key, Class<T> c, T fallback) {
        try {
            return Enum.valueOf(c, obj.get(key).getAsString().trim().toUpperCase());
        } catch (NullPointerException | UnsupportedOperationException | IllegalArgumentException e) {
            return fallback;
        }
    }

    public static JsonArray asArray(JsonObject obj, String key) {
        try {
            return obj.get(key).getAsJsonArray();
        } catch (NullPointerException | UnsupportedOperationException | IllegalStateException e) {
            return new JsonArray();
        }
    }

    public static JsonObject asObject(JsonObject obj, String key) {
        try {
            return obj.get(key).getAsJsonObject();
        } catch (NullPointerException | UnsupportedOperationException | IllegalStateException e) {
            return new JsonObject();
        }
    }

    public static Constraint asConstraint(JsonObject obj, String key) {
        JsonObject conObj = obj.getAsJsonObject(key);
        Constraint con = new Constraint();

        con.equal = Parser.asInt(conObj, "==", null);
        con.notEqual = Parser.asInt(conObj, "!=", null);
        con.lessThan = Parser.asInt(conObj, "<", null);
        con.lessThanOrEqual = Parser.asInt(conObj, "<=", null);
        con.greaterThan = Parser.asInt(conObj, ">", null);
        con.greaterThanOrEqual = Parser.asInt(conObj, ">=", null);

        return con;
    }

    public static ArrayList<String> asDimensionList(JsonObject obj, String key) {
        ArrayList<String> list = new ArrayList<String>();

        asArray(obj, key).forEach(e -> {
            if (e.isJsonPrimitive()) {
                JsonPrimitive primitive = e.getAsJsonPrimitive();
                if (primitive.isString()) list.add(primitive.getAsString());
            }
        });

        return list;
    }
}