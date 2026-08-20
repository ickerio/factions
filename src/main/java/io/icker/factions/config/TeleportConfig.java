package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

public class TeleportConfig {
    @SerializedName("enabled")
    public boolean ENABLED = true;

    @SerializedName("cooldownSeconds")
    public int COOLDOWN_SECONDS = 15;

    @SerializedName("requestExpirySeconds")
    public int REQUEST_EXPIRY_SECONDS = 60;

    @SerializedName("damageCooldown")
    public int DAMAGE_COOLDOWN = 100;
}
