package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Faction;

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