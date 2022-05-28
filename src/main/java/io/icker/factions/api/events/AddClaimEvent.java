package io.icker.factions.api.events;

import java.util.ArrayList;

import io.icker.factions.api.persistents.Claim;

public class AddClaimEvent {
    private static final ArrayList<AddClaimEventListener> listeners = new ArrayList<>();

    public static void register(AddClaimEventListener listener) {
        listeners.add(listener);
    }

    public static void run(Claim claim) {
        for (AddClaimEventListener listener : listeners) {
            listener.run(claim);
        }
    }

    public interface AddClaimEventListener {
        void run(Claim claim);
    }
}