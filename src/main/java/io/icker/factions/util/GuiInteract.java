package io.icker.factions.util;

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class GuiInteract {
    public static void playClickSound(ServerPlayerEntity player) {
        playSound(player, SoundEvent.of(Identifier.of("minecraft:ui.button.click")), 1, 1);
    }

    public static void playSound(
            ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        player.networkHandler.sendPacket(
                new PlaySoundS2CPacket(
                        Registries.SOUND_EVENT.getEntry(sound),
                        SoundCategory.UI,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        volume,
                        pitch,
                        player.getRandom().nextLong()));
    }
}
