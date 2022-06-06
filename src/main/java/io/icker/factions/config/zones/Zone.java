package io.icker.factions.config.zones;

public class Zone {
    private Dimensions dimensions;
    private Axis x;
    private Axis z;
    public Rules rules;

    public boolean isApplicable(String dimension, int xPos, int zPos) {
        return dimensions.validate(dimension) && x.validate(xPos) && z.validate(zPos);
    }
}
