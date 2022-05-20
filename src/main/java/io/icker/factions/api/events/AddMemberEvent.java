package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Member;

public class AddMemberEvent {
    private static final ArrayList<AddMemberEventListener> listeners = new ArrayList<>();

    public static void register(AddMemberEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Member member) {
        for (AddMemberEventListener listener : listeners) {
            listener.run(member);
        }
    }

    public interface AddMemberEventListener {
        void run(Member member);
    }
}