package io.icker.factions.core;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.GuiInteract;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class SoundManager {
    public static PlayerList playerManager;

    public static void register() {
        ClaimEvents.ADD.register(
                claim -> playFaction(claim.getFaction(), SoundEvents.NOTE_BLOCK_PLING, 2.0F));
        ClaimEvents.REMOVE.register(
                (x, z, level, faction) ->
                        playFaction(faction, SoundEvents.NOTE_BLOCK_PLING, 0.5F));
        FactionEvents.POWER_CHANGE.register(
                (faction, oldPower) ->
                        playFaction(faction, SoundEvents.NOTE_BLOCK_CHIME, 1F));
        FactionEvents.MEMBER_JOIN.register(
                (faction, user) -> playFaction(faction, SoundEvents.NOTE_BLOCK_BIT, 2.0F));
        FactionEvents.MEMBER_LEAVE.register(
                (faction, user) -> playFaction(faction, SoundEvents.NOTE_BLOCK_BIT, 0.5F));
    }

    private static void playFaction(
            Faction faction, Holder.Reference<SoundEvent> soundEvent, float pitch) {
        for (User user : faction.getUsers()) {
            ServerPlayer player = FactionsManager.playerManager.getPlayer(user.getID());
            if (player != null
                    && (user.sounds == User.SoundMode.ALL
                            || user.sounds == User.SoundMode.FACTION)) {
                GuiInteract.playSound(player, soundEvent.value(), 0.2F, pitch);
            }
        }
    }

    public static void warningSound(ServerPlayer player) {
        User user = User.get(player.getUUID());
        if (user.sounds == User.SoundMode.ALL || user.sounds == User.SoundMode.WARNINGS) {
            GuiInteract.playSound(player, SoundEvents.NOTE_BLOCK_BASS.value(), 0.5F, 1.0F);
        }
    }
}
