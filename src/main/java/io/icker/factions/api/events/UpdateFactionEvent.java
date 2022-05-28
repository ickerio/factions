package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Faction;

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