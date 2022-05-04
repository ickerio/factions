package io.icker.factions.api;

import io.icker.factions.database.Ally;

import java.util.ArrayList;

public class AllyRemoveEvent {
    private static final ArrayList<AllyRemoveEventListener> listeners = new ArrayList<>();

    public static void register(AllyRemoveEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Ally ally) {
        for (AllyRemoveEventListener listener : listeners) {
            listener.run(ally);
        }
    }

    public interface AllyRemoveEventListener {
        void run(Ally ally);
    }
}