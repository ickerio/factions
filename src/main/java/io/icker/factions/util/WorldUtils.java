package io.icker.factions.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class WorldUtils {
    public static MinecraftServer server;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register((server1 -> WorldUtils.server = server1));
    }

    public static boolean isValid(String level) {
        return WorldUtils.server.getWorldRegistryKeys().stream().anyMatch(key -> Objects.equals(key.getValue(), new Identifier(level)));
    }

    @Nullable
    public static ServerWorld getWorld(String level) {
        var key = WorldUtils.server.getWorldRegistryKeys().stream().filter(testKey -> Objects.equals(testKey.getValue(), new Identifier(level))).findAny();

        if (key.isEmpty()) {
            return null;
        } else {
            return server.getWorld(key.get());
        }
    }
}
