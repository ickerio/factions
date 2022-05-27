package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.User;

public class LeaveFactionEvent {
    private static final ArrayList<LeaveFactionEventListener> listeners = new ArrayList<>();

    public static void register(LeaveFactionEventListener listener) {
        listeners.add(listener);
    }

    public static void run(User user) {
        for (LeaveFactionEventListener listener : listeners) {
            listener.run(user);
        }
    }

    public interface LeaveFactionEventListener {
        void run(User user);
    }
}