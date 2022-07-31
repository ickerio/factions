package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

public class WarConfig {
    @SerializedName("attackAggression")
    public int ATTACK_AGGRESSION = 50;

    @SerializedName("aggressionLevelForWar")
    public int AGGRESSION_LEVEL = 100;
}
