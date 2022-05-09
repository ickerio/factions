package io.icker.factions.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public class PermissionsWrapper {
    public static boolean require(ServerCommandSource source, @NotNull String permission) {
        if (FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")) {
            return PermissionsInnerWrapper.check(source, permission, 0);
        } else {
            return true;
        }
    }

    public static boolean require(ServerCommandSource source, @NotNull String permission, int defaultValue) {
        if (FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")) {
            return PermissionsInnerWrapper.check(source, permission, defaultValue);
        } else {
            return source.hasPermissionLevel(defaultValue);
        }
    }

    public static boolean exists() {
        return FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
    }
}
