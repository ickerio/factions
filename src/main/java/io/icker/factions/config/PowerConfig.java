package io.icker.factions.config;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

public class PowerConfig {
    @SerializedName("base")
    public int BASE = 20;

    @SerializedName("member")
    public int MEMBER = 20;

    @SerializedName("claimWeight")
    public int CLAIM_WEIGHT = 5;

    @SerializedName("powerTicks")
    public PowerTicks POWER_TICKS = new PowerTicks();

    public static class PowerTicks {
        @SerializedName("deathPenalty")
        public int DEATH_PENALTY = 10;

        @SerializedName("ticks")
        public int TICKS = 12000;

        @SerializedName("reward")
        public int REWARD = 1;
    }

    public static class Deserializer implements JsonDeserializer<PowerConfig> {
        @Override
        public PowerConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject() && !json.getAsBoolean()) {
                return null;
            }

            return new Gson().fromJson(json, PowerConfig.class);
        }
    }
}
