package io.icker.factions.mixin;

import io.icker.factions.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(at = @At("HEAD"), method="save")
    public void save(boolean suppressLogs, boolean flush, boolean force, CallbackInfo ci) {
        ServerEvents.save();
    }
}
