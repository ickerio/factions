package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

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
}
