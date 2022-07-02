package io.icker.factions.config;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

public class HomeConfig {
    @SerializedName("claimOnly")
    public boolean CLAIM_ONLY = true;

    @SerializedName("damageTickCooldown")
    public int DAMAGE_COOLDOWN = 100;

    public static class Deserializer implements JsonDeserializer<HomeConfig> {
        @Override
        public HomeConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject() && !json.getAsBoolean()) {
                return null;
            }

            return new Gson().fromJson(json, HomeConfig.class);
        }
    }
}
