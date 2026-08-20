package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

public class AnnouncerConfig {
    @SerializedName("enabled")
    public boolean ENABLED = true;

    @SerializedName("displaySeconds")
    public int DISPLAY_SECONDS = 5;
}
