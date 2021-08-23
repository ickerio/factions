package io.icker.factions.config;

import java.util.List;

public class Zone {
    public static enum Type {
        DEFAULT,
        WILDERNESS,
        ADMIN;
    }

    Type type;
    String message;
    Constraint x;
    Constraint z;
    List<String> includedDimensions;
    List<String> excludedDimensions;

    public Zone(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public boolean isApplicable(String dimension, int x, int z) {
        return matchDimension(dimension) && matchCoords(x, z);
    }

    public boolean matchDimension(String dimension) {
        boolean included = includedDimensions.contains("*") || includedDimensions.contains(dimension);
        return excludedDimensions.contains(dimension) ? false : included;
    }

    public boolean matchCoords(int xPos, int zPos) {
        return x.validate(xPos) && z.validate(zPos);
    }

    public String getFailMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }
}
