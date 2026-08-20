package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class GuestGrantConfig {
    @SerializedName("maxBreak")
    public int MAX_BREAK = 64;

    @SerializedName("maxPlace")
    public int MAX_PLACE = 64;

    @SerializedName("restrictedItems")
    public List<String> RESTRICTED_ITEMS =
            new ArrayList<>(
                    List.of(
                            "minecraft:flint_and_steel",
                            "minecraft:fire_charge",
                            "minecraft:lava_bucket"));
}
