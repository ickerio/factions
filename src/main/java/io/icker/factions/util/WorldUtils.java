package io.icker.factions.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class WorldUtils {
    public static MinecraftServer server;

    public static final Event<Ready> ON_READY =
            EventFactory.createArrayBacked(
                    Ready.class,
                    callbacks ->
                            () -> {
                                for (Ready callback : callbacks) {
                                    callback.onReady();
                                }
                            });

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(
                (server1) -> {
                    WorldUtils.server = server1;
                    ON_READY.invoker().onReady();
                });
    }

    public static boolean isReady() {
        return server != null;
    }

    public static boolean hasWorlds() {
        return !WorldUtils.server.levelKeys().isEmpty();
    }

    public static boolean isValid(String level) {
        return WorldUtils.server.levelKeys().stream()
                .anyMatch(key -> Objects.equals(key.identifier(), Identifier.parse(level)));
    }

    @Nullable
    public static ServerLevel getWorld(String level) {
        Optional<ResourceKey<Level>> key =
                WorldUtils.server.levelKeys().stream()
                        .filter(testKey -> Objects.equals(testKey.identifier(), Identifier.parse(level)))
                        .findAny();

        if (key.isEmpty()) {
            return null;
        } else {
            return server.getLevel(key.get());
        }
    }

    @FunctionalInterface
    public interface Ready {
        void onReady();
    }
}
