package io.icker.factions.config;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ObjectParser extends Parser {
    protected Parser parent;

    public ObjectParser(JsonObject object, Parser parent) throws IOException {
        this.object = object;
        this.parent = parent;
    }

    protected void create(String key, JsonElement fallback) throws IOException {
        super.create(key, fallback);
        this.parent.update();
    }
}