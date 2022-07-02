package io.icker.factions.config;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

public class SafeConfig {
    @SerializedName("enderChest")
    public boolean ENDER_CHEST = true;

    @SerializedName("double")
    public boolean DOUBLE = true;

    public static class Deserializer implements JsonDeserializer<SafeConfig> {
        @Override
        public SafeConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject() && !json.getAsBoolean()) {
                return null;
            }

            return new Gson().fromJson(json, SafeConfig.class);
        }
    }
}
