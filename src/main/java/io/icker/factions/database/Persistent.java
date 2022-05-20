package io.icker.factions.database;

import java.util.HashMap;

public abstract class Persistent {
    public static final HashMap<String, Persistent> store = new HashMap<String, Persistent>();

    public abstract String getKey();
}