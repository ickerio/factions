package io.icker.factions.api;

import io.icker.factions.database.Claim;

import java.util.ArrayList;

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