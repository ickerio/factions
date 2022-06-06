package io.icker.factions.config.zones;

import com.google.gson.annotations.SerializedName;

public class Axis {
    @SerializedName("=")
    private Integer equal;

    @SerializedName("!=")
    private Integer notEqual;

    @SerializedName("<")
    private Integer lessThan;

    @SerializedName("<=")
    private Integer lessThanOrEqual;

    @SerializedName(">")
    private Integer greaterThan;

    @SerializedName(">=")
    private Integer greaterThanOrEqual;

    public boolean validate(int v) {
        return 
            checkEqual(v) && checkNotEqual(v) && 
            checkLessThan(v) && checkLessThanOrEqual(v) && 
            checkGreaterThan(v) && checkGreaterThanOrEqual(v);
    }

    private boolean checkEqual(int value) {
        return equal == null ? true : value == equal;
    }

    private boolean checkNotEqual(int value) {
        return notEqual == null ? true : value != notEqual;
    }

    private boolean checkLessThan(int value) {
        return lessThan == null ? true : value < lessThan;
    }

    private boolean checkLessThanOrEqual(int value) {
        return lessThanOrEqual == null ? true : value <= lessThanOrEqual;
    }

    private boolean checkGreaterThan(int value) {
        return greaterThan == null ? true : value > greaterThan;
    }

    private boolean checkGreaterThanOrEqual(int value) {
        return greaterThanOrEqual == null ? true : value >= greaterThanOrEqual;
    }
}
