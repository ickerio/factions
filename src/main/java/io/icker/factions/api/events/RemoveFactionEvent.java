package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Faction;

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