package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

public class TradeStageConfig {
    @SerializedName("enabled")
    public boolean ENABLED = true;

    @SerializedName("requesterX")
    public double REQUESTER_X = 1.050;

    @SerializedName("requesterY")
    public double REQUESTER_Y = 160.125;

    @SerializedName("requesterZ")
    public double REQUESTER_Z = -27.500;

    @SerializedName("requesterYaw")
    public float REQUESTER_YAW = 89.7f;

    @SerializedName("requesterPitch")
    public float REQUESTER_PITCH = 3.2f;

    @SerializedName("recipientX")
    public double RECIPIENT_X = -2.050;

    @SerializedName("recipientY")
    public double RECIPIENT_Y = 160.125;

    @SerializedName("recipientZ")
    public double RECIPIENT_Z = -27.482;

    @SerializedName("recipientYaw")
    public float RECIPIENT_YAW = -90.9f;

    @SerializedName("recipientPitch")
    public float RECIPIENT_PITCH = 3.2f;

    @SerializedName("level")
    public String LEVEL = "minecraft:overworld";
}
