package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    public void handleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        PlayerEvents.ON_MOVE.invoker().onMove(player);
    }

    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    public void broadcastChatMessage(PlayerChatMessage signedMessage, CallbackInfo ci) {
        User member = User.get(signedMessage.link().sender());

        boolean factionChat =
                member.chat == User.ChatMode.FACTION || member.chat == User.ChatMode.FOCUS;

        if (factionChat && !member.isInFaction()) {
            new Message(Component.translatable("factions.chat.faction_chat_when_not_in_faction"))
                    .fail()
                    .hover(
                            Component.translatable(
                                    "factions.chat.faction_chat_when_not_in_faction.hover"))
                    .click("/factions settings chat global")
                    .send(
                            WorldUtils.server
                                    .getPlayerList()
                                    .getPlayer(signedMessage.link().sender()),
                            false);

            ci.cancel();
        }
    }
}
