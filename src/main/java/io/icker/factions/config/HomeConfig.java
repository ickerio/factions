package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

public class HomeConfig {
    @SerializedName("claimOnly")
    public boolean CLAIM_ONLY = true;

    @SerializedName("damageTickCooldown")
    public int DAMAGE_COOLDOWN = 100;

    @SerializedName("homeWarpCooldownSecond")
    public int HOME_WARP_COOLDOWN_SECOND = 15;
}
