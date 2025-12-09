package io.icker.factions.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class GuiInteract {
    public static void playClickSound(ServerPlayerEntity player) {
        player.playSound(SoundEvent.of(Identifier.of("minecraft:ui.button.click")), 1, 1);
    }
}
