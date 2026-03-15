package io.icker.factions.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class GuiInteract {
    public static void playClickSound(ServerPlayer player) {
        playSound(player, SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:ui.button.click")), 1, 1);
    }

    public static void playSound(
            ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.connection.send(
                new ClientboundSoundPacket(
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                        SoundSource.UI,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        volume,
                        pitch,
                        player.getRandom().nextLong()));
    }
}
