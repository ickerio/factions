package io.icker.factions.api;

import io.icker.factions.database.Faction;

import java.util.ArrayList;

public class PowerChangeEvent {
    private static final ArrayList<PowerChangeListener> listeners = new ArrayList<>();

    public static void register(PowerChangeListener listener) {
        listeners.add(listener);
    }

    public static void run(Faction faction) {
        for (PowerChangeListener listener : listeners) {
            listener.run(faction);
        }
    }

    public interface PowerChangeListener {
        void run(Faction faction);
    }
}