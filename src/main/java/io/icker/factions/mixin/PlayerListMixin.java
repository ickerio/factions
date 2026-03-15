package io.icker.factions.mixin;

import io.icker.factions.api.persistents.User;
import io.icker.factions.util.StyledChatCompatibility;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Redirect(
            method =
                    "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerPlayer;sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"))
    public void sendChatMessage(
            ServerPlayer player,
            OutgoingChatMessage message,
            boolean bl,
            ChatType.Bound parameters) {
        if (message instanceof OutgoingChatMessage.Disguised
                || (FabricLoader.getInstance().isModLoaded("styledchat")
                        && StyledChatCompatibility.isNotPlayer(message))) {
            player.sendChatMessage(message, bl, parameters);
            return;
        }

        User sender;

        if (FabricLoader.getInstance().isModLoaded("styledchat")) {
            sender = User.get(StyledChatCompatibility.getSender(message));
        } else {
            sender = User.get(((OutgoingChatMessage.Player) message).message().link().sender());
        }

        User target = User.get(player.getUUID());

        if (sender.chat == User.ChatMode.GLOBAL && target.chat != User.ChatMode.FOCUS) {
            player.sendChatMessage(message, bl, parameters);
        }

        if ((sender.chat == User.ChatMode.FACTION || sender.chat == User.ChatMode.FOCUS)
                && sender.getFaction().equals(target.getFaction())) {
            player.sendChatMessage(message, bl, parameters);
        }
    }
}
