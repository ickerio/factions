package io.icker.factions.mixin;

import io.icker.factions.event.ChatEvents;
import net.minecraft.network.ChatDecorator;
import net.minecraft.server.MinecraftServer;
import io.icker.factions.event.ServerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

import io.icker.factions.core.ServerEvents;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "getChatDecorator", at = @At("HEAD"), cancellable = true)
    public void getChatDecorator(CallbackInfoReturnable<ChatDecorator> cir) {
        cir.setReturnValue((sender, message) -> {
            return CompletableFuture.completedFuture(ChatEvents.handleMessage(sender, message.getString()));
        });
    }

    @Inject(at = @At("HEAD"), method="Lnet/minecraft/server/MinecraftServer;save(ZZZ)Z")
    public void save(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> ci) {
        ServerEvents.save();
    }
}
