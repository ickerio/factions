package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.event.ChatEvents;
import io.icker.factions.event.PlayerInteractEvents;
import net.minecraft.network.ChatDecorator;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.Formatting;
import io.icker.factions.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "getChatDecorator", at = @At("HEAD"), cancellable = true)
    public void getChatDecorator(CallbackInfoReturnable<ChatDecorator> cir) {
        cir.setReturnValue((sender, message) -> {
            return CompletableFuture.completedFuture(ChatEvents.handleMessage(sender, message.getString()));
        });
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(at = @At("HEAD"), method="Lnet/minecraft/server/MinecraftServer;save(ZZZ)Z")
    public void save(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> ci) {
        ServerEvents.save();
    }
}
