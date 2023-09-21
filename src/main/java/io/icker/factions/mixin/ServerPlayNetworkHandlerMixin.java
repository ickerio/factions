package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.entity.Entity;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Final
    @Shadow
    private MinecraftServer server;

    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    public void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        PlayerEvents.ON_MOVE.invoker().onMove(player);
    }

    @Inject(method = "handleDecoratedMessage", at = @At("HEAD"), cancellable = true)
    public void handleDecoratedMessage(SignedMessage signedMessage, CallbackInfo ci) {
        User member = User.get(signedMessage.link().sender());

        boolean factionChat =
                member.chat == User.ChatMode.FACTION || member.chat == User.ChatMode.FOCUS;

        if (factionChat && !member.isInFaction()) {
            new Message("You can't send a message to faction chat if you aren't in a faction.")
                    .fail().hover("Click to switch to global chat")
                    .click("/factions settings chat global")
                    .send(server.getPlayerManager().getPlayer(signedMessage.link().sender()),
                            false);

            ci.cancel();
        }
    }

    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    public void onPlayerInteractEntity(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        World world = player.getWorld();
        Entity entity = packet.getEntity((ServerWorld) world);
        if (entity == null)
            return;

        packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
            @Override
            public void interact(Hand hand) {
                if (PlayerEvents.USE_ENTITY.invoker().onUseEntity(player, entity,
                        world) == ActionResult.FAIL) {
                    ci.cancel();
                }
            }

            @Override
            public void interactAt(Hand hand, Vec3d pos) {
                if (PlayerEvents.USE_ENTITY.invoker().onUseEntity(player, entity,
                        world) == ActionResult.FAIL) {
                    ci.cancel();
                }
            }

            @Override
            public void attack() {}
        });
    }
}
