package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Relationship;

// TODO its unused
public class RelationshipEndEvent {
    private static final ArrayList<RelationshipEndEventListener> listeners = new ArrayList<>();

    public static void register(RelationshipEndEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Relationship rel) {
        for (RelationshipEndEventListener listener : listeners) {
            listener.run(rel);
        }
    }

    public interface RelationshipEndEventListener {
        void run(Relationship rel);
    }
}