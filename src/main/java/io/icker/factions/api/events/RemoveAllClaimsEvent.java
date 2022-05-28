package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Faction;

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