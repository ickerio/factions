package io.icker.factions.mixin;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.database.PlayerConfig;
import io.icker.factions.event.PlayerInteractEvents;
import io.icker.factions.util.Message;
import net.minecraft.network.MessageType;
import net.minecraft.network.encryption.SignedChatMessage;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    public void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        PlayerInteractEvents.onMove(((ServerPlayNetworkHandler) (Object) this).player);
    }

    @Redirect(method = "handleDecoratedMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/server/filter/FilteredMessage;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/registry/RegistryKey;)V"))
    private void replaceChatMessage(PlayerManager instance, FilteredMessage<SignedChatMessage> message, ServerPlayerEntity sender, RegistryKey<MessageType> typeKey) {
        Member member = Member.get(sender.getUuid());
        Faction faction = member != null ? member.getFaction() : null;
        PlayerConfig.ChatOption chatOption = PlayerConfig.get(sender.getUuid()).chat;

        boolean factionChat = chatOption == PlayerConfig.ChatOption.FACTION || chatOption == PlayerConfig.ChatOption.FOCUS;

        if (factionChat && faction == null) {
            new Message("You can't send a message to faction chat if you aren't in a faction").fail().hover("Click to switch to global chat").click("/factions chat global").send(sender, false);
        } else {
            instance.broadcast(message.raw(), (player) -> {
                Member targetMember = Member.get(player.getUuid());
                Faction target = targetMember != null ? targetMember.getFaction() : null;

                if (chatOption == PlayerConfig.ChatOption.GLOBAL && PlayerConfig.get(player.getUuid()).chat != PlayerConfig.ChatOption.FOCUS) {
                    return message.getFilterableFor(sender, player);
                }

                if (factionChat && target != null && target.name.equals(faction.name)) {
                    return message.getFilterableFor(sender, player);
                }

                return null;
            }, sender.asMessageSender(), typeKey);
        }
    }
}
