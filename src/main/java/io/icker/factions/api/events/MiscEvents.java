package io.icker.factions.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

public final class MiscEvents {
    public static final Event<OnSave> ON_SAVE = EventFactory.createArrayBacked(OnSave.class, callbacks -> (server) -> {
        for (OnSave callback : callbacks) {
            callback.onSave(server);
        }
    });

    public static final Event<OnMobSpawnAttempt> ON_MOB_SPAWN_ATTEMPT = EventFactory.createArrayBacked(OnMobSpawnAttempt.class, callbacks -> () -> {
        for (OnMobSpawnAttempt callback : callbacks) {
            callback.onMobSpawnAttempt();
        }
    });

    @FunctionalInterface
    public interface OnSave {
        void onSave(MinecraftServer server);
    }

    @FunctionalInterface
    public interface OnMobSpawnAttempt { //TODO Implement this
        void onMobSpawnAttempt();
    }
}
