package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

public class GatherConfig {
    @SerializedName("enabled")
    public boolean ENABLED = true;

    @SerializedName("x")
    public double X = -0.552;

    @SerializedName("y")
    public double Y = 160.0;

    @SerializedName("z")
    public double Z = -18.300;

    @SerializedName("yaw")
    public float YAW = 179.6f;

    @SerializedName("pitch")
    public float PITCH = 4.7f;

    @SerializedName("level")
    public String LEVEL = "minecraft:overworld";

    @SerializedName("requestExpirySeconds")
    public int REQUEST_EXPIRY_SECONDS = 60;
}
