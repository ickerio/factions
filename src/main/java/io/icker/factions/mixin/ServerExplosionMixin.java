package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "calculateExplodedPositions", at = @At("TAIL"))
    private void canDestroyBlock(CallbackInfoReturnable<List<BlockPos>> cir) {
        Explosion explosion = (Explosion) this;

        cir.getReturnValue().removeIf((b) -> PlayerEvents.EXPLODE_BLOCK.invoker().onExplodeBlock(explosion, level, b) == InteractionResult.FAIL);
    }

    @Redirect(
            method = "hurtEntities",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;ignoreExplosion(Lnet/minecraft/world/level/Explosion;)Z"))
    public boolean hurtEntities(Entity entity, Explosion explosion) {
        InteractionResult result =
                PlayerEvents.EXPLODE_DAMAGE.invoker().onExplodeDamage(explosion, entity);
        if (result.consumesAction()) {
            return false;
        } else if (result == InteractionResult.FAIL) {
            return true;
        }

        return entity.ignoreExplosion(explosion);
    }
}
