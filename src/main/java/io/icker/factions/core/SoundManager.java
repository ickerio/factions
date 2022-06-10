package io.icker.factions.core;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundEvent;

public class SoundManager {
    public static PlayerManager playerManager;

    public static void register() { // TODO decide on better sounds to use, and fix overlapping sounds (eg. removing all claims)
        ClaimEvents.ADD.register(claim -> playFaction(claim.getFaction(), SoundEvents.BLOCK_NOTE_BLOCK_PLING, 2.0F));
        ClaimEvents.REMOVE.register((x, z, level, faction) -> playFaction(faction, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.5F));
        FactionEvents.POWER_CHANGE.register((faction, oldPower) -> {
            if (faction.getPower() > oldPower) playFaction(faction, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0F);
            else playFaction(faction, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F);
        });
        FactionEvents.MEMBER_JOIN.register((faction, user) -> playFaction(faction, SoundEvents.BLOCK_NOTE_BLOCK_BIT, 2.0F));
        FactionEvents.MEMBER_LEAVE.register((faction, user) -> playFaction(faction, SoundEvents.BLOCK_NOTE_BLOCK_BIT, 0.5F));
    }

    private static void playFaction(Faction faction, SoundEvent soundEvent, float pitch) {
        for (User user : faction.getUsers()) {
            ServerPlayerEntity player = FactionsManager.playerManager.getPlayer(user.getID());
            if (player != null && (user.sounds == User.SoundMode.ALL || user.sounds == User.SoundMode.FACTION)) {
                player.playSound(soundEvent, SoundCategory.PLAYERS, 1.0F, pitch);
            }
        }
    }

    public static void warningSound(ServerPlayerEntity player) {
        User user = User.get(player.getUuid());
        if (user.sounds == User.SoundMode.ALL || user.sounds == User.SoundMode.WARNINGS) {
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
        }
    }
}
