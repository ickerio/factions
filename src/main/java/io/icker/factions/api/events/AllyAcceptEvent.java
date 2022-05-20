package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Ally;

public class AllyAcceptEvent {
    private static final ArrayList<AllyAcceptEventListener> listeners = new ArrayList<>();

    public static void register(AllyAcceptEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Ally ally) {
        for (AllyAcceptEventListener listener : listeners) {
            listener.run(ally);
        }
    }

    public interface AllyAcceptEventListener {
        void run(Ally ally);
    }
}