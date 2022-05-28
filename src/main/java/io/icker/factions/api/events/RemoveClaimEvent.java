package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Claim;

public class RemoveClaimEvent {
    private static final ArrayList<RemoveClaimEventListener> listeners = new ArrayList<>();

    public static void register(RemoveClaimEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Claim claim) {
        for (RemoveClaimEventListener listener : listeners) {
            listener.run(claim);
        }
    }

    public interface RemoveClaimEventListener {
        void run(Claim claim);
    }
}