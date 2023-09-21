package io.icker.factions.core;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class SoundManager {
    public static PlayerManager playerManager;

    public static void register() {
        ClaimEvents.ADD.register(
                claim -> playFaction(claim.getFaction(), SoundEvents.BLOCK_NOTE_BLOCK_PLING, 2.0F));
        ClaimEvents.REMOVE.register((x, z, level, faction) -> playFaction(faction,
                SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.5F));
        FactionEvents.POWER_CHANGE.register((faction, oldPower) -> playFaction(faction,
                SoundEvents.BLOCK_NOTE_BLOCK_CHIME, 1F));
        FactionEvents.MEMBER_JOIN.register(
                (faction, user) -> playFaction(faction, SoundEvents.BLOCK_NOTE_BLOCK_BIT, 2.0F));
        FactionEvents.MEMBER_LEAVE.register(
                (faction, user) -> playFaction(faction, SoundEvents.BLOCK_NOTE_BLOCK_BIT, 0.5F));
    }

    private static void playFaction(Faction faction, RegistryEntry.Reference<SoundEvent> soundEvent,
            float pitch) {
        for (User user : faction.getUsers()) {
            PlayerEntity player = FactionsManager.playerManager.getPlayer(user.getID());
            if (player != null && (user.sounds == User.SoundMode.ALL
                    || user.sounds == User.SoundMode.FACTION)) {
                player.playSound(soundEvent.value(), SoundCategory.PLAYERS, 0.2F, pitch);
            }
        }
    }

    public static void warningSound(PlayerEntity player) {
        User user = User.get(player.getUuid());
        if (user.sounds == User.SoundMode.ALL || user.sounds == User.SoundMode.WARNINGS) {
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 0.5F,
                    1.0F);
        }
    }
}
