package io.icker.factions.api;

import io.icker.factions.database.Faction;

import java.util.ArrayList;

public class RemoveAllClaimsEvent {
    private static final ArrayList<RemoveAllClaimsEventListener> listeners = new ArrayList<>();

    public static void register(RemoveAllClaimsEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Faction faction) {
        for (RemoveAllClaimsEventListener listener : listeners) {
            listener.run(faction);
        }
    }

    public interface RemoveAllClaimsEventListener {
        void run(Faction faction);
    }
}