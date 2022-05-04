package io.icker.factions.api;

import io.icker.factions.database.Faction;

import java.util.ArrayList;

public class UpdateFactionEvent {
    private static final ArrayList<UpdateFactionEventListener> listeners = new ArrayList<>();

    public static void register(UpdateFactionEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Faction faction) {
        for (UpdateFactionEventListener listener : listeners) {
            listener.run(faction);
        }
    }

    public interface UpdateFactionEventListener {
        void run(Faction faction);
    }
}