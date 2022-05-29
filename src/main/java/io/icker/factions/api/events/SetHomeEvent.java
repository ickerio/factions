package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;

public class SetHomeEvent {
    private static final ArrayList<SetHomeEventListener> listeners = new ArrayList<>();

    public static void register(SetHomeEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Faction faction, Home home) {
        for (SetHomeEventListener listener : listeners) {
            listener.run(faction, home);
        }
    }

    public interface SetHomeEventListener {
        void run(Faction faction, Home home);
    }
}