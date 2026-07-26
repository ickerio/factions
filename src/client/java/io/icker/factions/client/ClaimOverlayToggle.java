package io.icker.factions.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClaimOverlayToggle {
    private ClaimOverlayToggle() {}

    private static volatile boolean worldMapEnabled = true;
    private static volatile boolean inWorldMap;
    private static KeyMapping toggleKey;

    public static boolean isWorldMapEnabled() {
        return worldMapEnabled;
    }

    public static void register() {
        toggleKey = new KeyMapping(
                "key.factions.toggle_worldmap_claims",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_J,
                KeyMapping.Category.MISC);
        KeyMappingHelper.registerKeyMapping(toggleKey);
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!"xaero.map.gui.GuiMap".equals(screen.getClass().getName())) return;
            inWorldMap = true;
            ScreenEvents.remove(screen).register(s -> inWorldMap = false);
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, keyEvent) -> {
                int bound = KeyMappingHelper.getBoundKeyOf(toggleKey).getValue();
                if (keyEvent.key() != bound) return;
                worldMapEnabled = !worldMapEnabled;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.sendOverlayMessage(
                            Component.literal("Faction claim overlay on World Map: " + (worldMapEnabled ? "ON" : "OFF")));
                }
            });
        });
    }
}
