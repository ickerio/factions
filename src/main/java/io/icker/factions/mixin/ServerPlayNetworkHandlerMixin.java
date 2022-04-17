package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.icker.factions.event.PlayerInteractEvents;

import net.minecraft.client.gui.hud.InGameHud;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    public void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        PlayerInteractEvents.onMove(((ServerPlayNetworkHandler)(Object)this).player);
    }
}
