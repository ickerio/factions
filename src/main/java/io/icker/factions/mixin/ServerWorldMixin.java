package io.icker.factions.mixin;

import io.icker.factions.event.EntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    public void spawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!EntityEvents.entitySpawn(entity))
            cir.setReturnValue(false);
    }
}
