package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Home;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class HomeEvents {
    public static final Event<Set> SET = EventFactory.createArrayBacked(Set.class, callbacks -> (home) -> {
        for (Set callback : callbacks) {
            callback.onSet(home);
        }
    });

    @FunctionalInterface
    public interface Set {
        void onSet(Home home);
    }
}
