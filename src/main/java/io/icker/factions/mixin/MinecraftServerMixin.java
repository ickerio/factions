package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.icker.factions.api.events.MiscEvents;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/server/MinecraftServer;save(ZZZ)Z")
    public void save(boolean suppressLogs, boolean flush, boolean force,
            CallbackInfoReturnable<Boolean> ci) {
        MiscEvents.ON_SAVE.invoker().onSave((MinecraftServer) (Object) this);
    }
}
