package io.icker.factions.api;

import io.icker.factions.database.Faction;

import java.util.ArrayList;

public class RemoveFactionEvent {
    private static final ArrayList<RemoveFactionEventListener> listeners = new ArrayList<>();

    public static void register(RemoveFactionEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Faction faction) {
        for (RemoveFactionEventListener listener : listeners) {
            listener.run(faction);
        }
    }

    public interface RemoveFactionEventListener {
        void run(Faction faction);
    }
}