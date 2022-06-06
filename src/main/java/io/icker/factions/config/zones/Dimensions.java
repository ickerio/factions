package io.icker.factions.config.zones;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class Dimensions {
    @SerializedName("include")
    private ArrayList<String> included;

    @SerializedName("exclude")
    private ArrayList<String> excluded;

    public boolean validate(String dimension) {
        return excluded.contains(dimension) ? false : (included.contains("*") || included.contains(dimension));
    }
}
