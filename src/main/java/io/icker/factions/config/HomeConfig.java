package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

public class HomeConfig {
    @SerializedName("claimOnly")
    public boolean CLAIM_ONLY = true;

    @SerializedName("damageTickCooldown")
    public int DAMAGE_COOLDOWN = 100;
}
