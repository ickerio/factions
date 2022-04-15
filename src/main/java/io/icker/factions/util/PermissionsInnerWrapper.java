package io.icker.factions.util;

import me.lucko.fabric.api.permissions.v0.Permissions;
import org.jetbrains.annotations.NotNull;
import net.minecraft.server.command.ServerCommandSource;

public class PermissionsInnerWrapper {
  public static boolean check(ServerCommandSource source, @NotNull String permission, int defaultValue) {
    return Permissions.check(source, permission, defaultValue);
  }

  public static void exists() {
    ;
  }
}
