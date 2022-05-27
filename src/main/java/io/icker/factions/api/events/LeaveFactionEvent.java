package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.User;

public class LeaveFactionEvent {
    private static final ArrayList<RemoveMemberEventListener> listeners = new ArrayList<>();

    public static void register(RemoveMemberEventListener listener) {
        listeners.add(listener);
    }

    public static void run(User member) {
        for (RemoveMemberEventListener listener : listeners) {
            listener.run(member);
        }
    }

    public interface RemoveMemberEventListener {
        void run(User member);
    }
}