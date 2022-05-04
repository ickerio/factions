package io.icker.factions.api;

import io.icker.factions.database.Member;

import java.util.ArrayList;

public class RemoveMemberEvent {
    private static final ArrayList<RemoveMemberEventListener> listeners = new ArrayList<>();

    public static void register(RemoveMemberEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Member member) {
        for (RemoveMemberEventListener listener : listeners) {
            listener.run(member);
        }
    }

    public interface RemoveMemberEventListener {
        void run(Member member);
    }
}