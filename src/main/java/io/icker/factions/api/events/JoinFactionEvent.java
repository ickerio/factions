package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.User;

public class JoinFactionEvent {
    private static final ArrayList<JoinFactionEventListener> listeners = new ArrayList<>();

    public static void register(JoinFactionEventListener listener) {
        listeners.add(listener);
    }

    public static void run(User user) {
        for (JoinFactionEventListener listener : listeners) {
            listener.run(user);
        }
    }

    public interface JoinFactionEventListener {
        void run(User user);
    }
}