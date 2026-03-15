package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    public void onPlayerMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        PlayerEvents.ON_MOVE.invoker().onMove(player);
    }

    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    public void handleDecoratedMessage(PlayerChatMessage signedMessage, CallbackInfo ci) {
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

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    public void onPlayerInteractEntity(ServerboundInteractPacket packet, CallbackInfo ci) {
        Level world = player.level();
        Entity entity = packet.getTarget((ServerLevel) world);
        if (entity == null) return;

        packet.dispatch(
                new ServerboundInteractPacket.Handler() {
                    @Override
                    public void onInteraction(InteractionHand hand) {
                        if (PlayerEvents.USE_ENTITY.invoker().onUseEntity(player, entity, world)
                                == InteractionResult.FAIL) {
                            ci.cancel();
                        }
                    }

                    @Override
                    public void onInteraction(InteractionHand hand, Vec3 pos) {
                        if (PlayerEvents.USE_ENTITY.invoker().onUseEntity(player, entity, world)
                                == InteractionResult.FAIL) {
                            ci.cancel();
                        }
                    }

                    @Override
                    public void onAttack() {}
                });
    }
}
