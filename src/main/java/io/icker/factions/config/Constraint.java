package io.icker.factions.config;

public class Constraint {
    public Integer equal; // ==
    public Integer notEqual; // !=
    public Integer lessThan; // <
    public Integer lessThanOrEqual; // <=
    public Integer greaterThan; // >
    public Integer greaterThanOrEqual; // >=

    public boolean validate(int v) {
        return 
            checkEqual(v) && checkNotEqual(v) && 
            checkLessThan(v) && checkLessThanOrEqual(v) && 
            checkGreaterThan(v) && checkGreaterThanOrEqual(v);
    }

    public boolean checkEqual(int value) {
        return equal == null ? true : value == equal;
    }

    public boolean checkNotEqual(int value) {
        return notEqual == null ? true : value != notEqual;
    }

    public boolean checkLessThan(int value) {
        return lessThan == null ? true : value < lessThan;
    }

    public boolean checkLessThanOrEqual(int value) {
        return lessThanOrEqual == null ? true : value <= lessThanOrEqual;
    }

    public boolean checkGreaterThan(int value) {
        return greaterThan == null ? true : value > greaterThan;
    }

    public boolean checkGreaterThanOrEqual(int value) {
        return greaterThanOrEqual == null ? true : value >= greaterThanOrEqual;
    }
}