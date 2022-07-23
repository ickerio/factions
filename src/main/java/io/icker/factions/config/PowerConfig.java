package io.icker.factions.config;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class PowerConfig {

    @SerializedName("member")
    public int MEMBER = 20;

    @SerializedName("claimWeight")
    public int CLAIM_WEIGHT = 2;

    @SerializedName("deathPenalty")
    public int DEATH_PENALTY = 5;

    @SerializedName("powerTicks")
    public PowerTicks POWER_TICKS = new PowerTicks();

    @SerializedName("pveDeathPenalty")
    public boolean PVE_DEATH_PENALTY = false;

    public static class PowerTicks {
        @SerializedName("ticks")
        public int TICKS = 12000;

        @SerializedName("reward")
        public int REWARD = 3;
    }

    public static class Deserializer implements JsonDeserializer<PowerConfig> {
        @Override
        public PowerConfig deserialize(@NotNull JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject() && !json.getAsBoolean()) {
                return null;
            }

            return new Gson().fromJson(json, PowerConfig.class);
        }
    }
}
