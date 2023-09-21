package io.icker.factions.util;

import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class WorldUtils {
    public static MinecraftServer server;

    public static final Event<Ready> ON_READY =
            EventFactory.createArrayBacked(Ready.class, callbacks -> () -> {
                for (Ready callback : callbacks) {
                    callback.onReady();
                }
            });

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register((server1) -> {
            WorldUtils.server = server1;
            ON_READY.invoker().onReady();
        });
    }

    public static boolean isReady() {
        return server != null;
    }

    public static boolean isValid(String level) {
        return WorldUtils.server.getWorldRegistryKeys().stream()
                .anyMatch(key -> Objects.equals(key.getValue(), new Identifier(level)));
    }

    @Nullable
    public static ServerWorld getWorld(String level) {
        Optional<RegistryKey<World>> key = WorldUtils.server.getWorldRegistryKeys().stream()
                .filter(testKey -> Objects.equals(testKey.getValue(), new Identifier(level)))
                .findAny();

        if (key.isEmpty()) {
            return null;
        } else {
            return server.getWorld(key.get());
        }
    }

    @FunctionalInterface
    public interface Ready {
        void onReady();
    }
}
