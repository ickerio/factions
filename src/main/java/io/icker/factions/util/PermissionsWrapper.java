package io.icker.factions.util;

import org.jetbrains.annotations.NotNull;
import net.minecraft.server.command.ServerCommandSource;

public class PermissionsWrapper {
  public static boolean require(ServerCommandSource source, @NotNull String permission) {
    try {
      return PermissionsInnerWrapper.check(source, permission);
    } catch (java.lang.NoClassDefFoundError e) {
      return true;
    }
  }
  public static boolean exists() {
    try {
      PermissionsInnerWrapper.exists();
      return true;
    } catch (java.lang.NoClassDefFoundError e) {
      return false;
    }
  }
}
