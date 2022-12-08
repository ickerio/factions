package io.icker.factions.mixin;

import io.icker.factions.api.persistents.User;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Redirect(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendChatMessage(Lnet/minecraft/network/message/SentMessage;ZLnet/minecraft/network/message/MessageType$Parameters;)V"))
    public void sendChatMessage(ServerPlayerEntity player, SentMessage message, boolean bl, MessageType.Parameters parameters) {
        if (message instanceof SentMessage.Profileless) {
            player.sendChatMessage(message, bl, parameters);
            return;
        }
        
        User sender = User.get(((SentMessage.Chat)message).message().link().sender());

        User target = User.get(player.getUuid());

        if (sender.chat == User.ChatMode.GLOBAL && target.chat != User.ChatMode.FOCUS) {
            player.sendChatMessage(message, bl, parameters);
        }

        if ((sender.chat == User.ChatMode.FACTION || sender.chat == User.ChatMode.FOCUS)&& sender.getFaction().equals(target.getFaction())) {
            player.sendChatMessage(message, bl, parameters);
        }
    }
}
