package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

public class WarConfig {
    @SerializedName("attackAggression")
    public int ATTACK_AGGRESSION = 50;

    @SerializedName("aggressionLevelForWar")
    public int AGGRESSION_LEVEL = 100;

    @SerializedName("trespassingTime")
    public int TRESPASSING_TIME = 70;

    @SerializedName("trespassingAggression")
    public int TRESPASSING_AGGRESSION = 7;
}
