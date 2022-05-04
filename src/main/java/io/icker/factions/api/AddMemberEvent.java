package io.icker.factions.api;

import io.icker.factions.database.Member;

import java.util.ArrayList;

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