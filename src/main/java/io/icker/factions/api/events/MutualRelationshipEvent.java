package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Relationship;


public class MutualRelationshipEvent {
    private static final ArrayList<MutualRelationshipEventListener> listeners = new ArrayList<>();

    public static void register(MutualRelationshipEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Relationship rel) {
        for (MutualRelationshipEventListener listener : listeners) {
            listener.run(rel);
        }
    }

    public interface MutualRelationshipEventListener {
        void run(Relationship rel);
    }
}