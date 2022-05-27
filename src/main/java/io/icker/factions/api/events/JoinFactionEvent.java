package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.User;

public class JoinFactionEvent {
    private static final ArrayList<AddMemberEventListener> listeners = new ArrayList<>();

    public static void register(AddMemberEventListener listener) {
        listeners.add(listener);
    }

    public static void run(User member) {
        for (AddMemberEventListener listener : listeners) {
            listener.run(member);
        }
    }

    public interface AddMemberEventListener {
        void run(User member);
    }
}