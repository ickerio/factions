package io.icker.factions.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

/**
 * Events related to miscellaneous actions
 */
public final class MiscEvents {
    /**
     * Called when the Factions database is saved (which is also when the server saves world and
     * player files)
     */
    public static final Event<Save> ON_SAVE =
            EventFactory.createArrayBacked(Save.class, callbacks -> (server) -> {
                for (Save callback : callbacks) {
                    callback.onSave(server);
                }
            });

    /**
     * Called when the game attempts to spawn in mobs (UNIMPLEMENTED)
     */
    public static final Event<MobSpawnAttempt> ON_MOB_SPAWN_ATTEMPT =
            EventFactory.createArrayBacked(MobSpawnAttempt.class, callbacks -> () -> {
                for (MobSpawnAttempt callback : callbacks) {
                    callback.onMobSpawnAttempt();
                }
            });

    @FunctionalInterface
    public interface Save {
        void onSave(MinecraftServer server);
    }

    @FunctionalInterface
    public interface MobSpawnAttempt {
        void onMobSpawnAttempt();
    }
}
