package io.icker.factions.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class GuiInteract {
    public static void playClickSound(ServerPlayerEntity player) {
        player.playSoundToPlayer(
                SoundEvent.of(Identifier.of("minecraft:ui.button.click")),
                SoundCategory.MASTER,
                1,
                1);
    }
}
