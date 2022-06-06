package io.icker.factions.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

public final class MiscEvents {
    public static final Event<Save> ON_SAVE = EventFactory.createArrayBacked(Save.class, callbacks -> (server) -> {
        for (Save callback : callbacks) {
            callback.onSave(server);
        }
    });

    public static final Event<MobSpawnAttempt> ON_MOB_SPAWN_ATTEMPT = EventFactory.createArrayBacked(MobSpawnAttempt.class, callbacks -> () -> {
        for (MobSpawnAttempt callback : callbacks) {
            callback.onMobSpawnAttempt();
        }
    });

    @FunctionalInterface
    public interface Save {
		void onSave(MinecraftServer server);
	}

    @FunctionalInterface
    public interface MobSpawnAttempt { //TODO Implement this
        void onMobSpawnAttempt();
    }
}
