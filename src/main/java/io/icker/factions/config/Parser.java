package io.icker.factions.config;

import com.google.gson.*;

import java.io.IOException;

public abstract class Parser {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    protected JsonObject object;

    protected void update() throws IOException {
    }

    protected void create(String key, JsonElement fallback) throws IOException {
        this.object.add(key, fallback);
    }

    public Integer getInteger(String key, Integer fallback) throws IOException {
        try {
            return this.object.get(key).getAsInt();
        } catch (NullPointerException | UnsupportedOperationException e) {
            if (fallback == null) {
                return null;
            }
            this.create(key, new JsonPrimitive(fallback));
            return fallback;
        }
    }

    public Integer getInteger(String key) throws IOException {
        return this.getInteger(key, 0);
    }

    public String getString(String key, String fallback) throws IOException {
        try {
            return this.object.get(key).getAsString();
        } catch (NullPointerException | UnsupportedOperationException e) {
            if (fallback == null) {
                return null;
            }
            this.create(key, new JsonPrimitive(fallback));
            return fallback;
        }
    }

    public String getString(String key) throws IOException {
        return this.getString(key, "");
    }

    public Boolean getBoolean(String key, Boolean fallback) throws IOException {
        try {
            return this.object.get(key).getAsBoolean();
        } catch (NullPointerException | UnsupportedOperationException e) {
            if (fallback == null) {
                return null;
            }
            this.create(key, new JsonPrimitive(fallback));
            return fallback;
        }
    }

    public <T extends Enum<T>> T getEnum(String key, Class<T> c, String fallback) throws IOException {
        try {
            return Enum.valueOf(c, this.getString(key, fallback).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            if (fallback == null) {
                return null;
            }
            this.create(key, new JsonPrimitive(fallback));
            return Enum.valueOf(c, fallback.trim().toUpperCase());
        }
    }

    public JsonArray getArray(String key) throws IOException {
        try {
            return this.object.get(key).getAsJsonArray();
        } catch (NullPointerException | UnsupportedOperationException | IllegalStateException e) {
            create(key, new JsonArray());
            return new JsonArray();
        }
    }

    public ObjectParser getParser(String key) throws IOException {
        try {
            return new ObjectParser(this.object.get(key).getAsJsonObject(), this);
        } catch (NullPointerException | UnsupportedOperationException | IllegalStateException e) {
            JsonObject newObject = new JsonObject();
            create(key, newObject);
            return new ObjectParser(newObject, this);
        }
    }
}