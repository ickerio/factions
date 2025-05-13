package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.explosion.ExplosionImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExplosionImpl.class)
public class ExplosionBehaviorMixin {
    @Redirect(
            method = "getBlocksToDestroy",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/explosion/ExplosionBehavior;canDestroyBlock(Lnet/minecraft/world/explosion/Explosion;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;F)Z"))
    public boolean canDestroyBlock(
            ExplosionBehavior behavior,
            Explosion explosion,
            BlockView world,
            BlockPos pos,
            BlockState state,
            float power) {
        ActionResult result =
                PlayerEvents.EXPLODE_BLOCK.invoker().onExplodeBlock(explosion, world, pos, state);
        if (result.isAccepted()) {
            return true;
        } else if (result == ActionResult.FAIL) {
            return false;
        }

        return behavior.canDestroyBlock(explosion, world, pos, state, power);
    }

    @Redirect(
            method = "damageEntities",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/entity/Entity;isImmuneToExplosion(Lnet/minecraft/world/explosion/Explosion;)Z"))
    public boolean shouldDamage(Entity entity, Explosion explosion) {
        ActionResult result =
                PlayerEvents.EXPLODE_DAMAGE.invoker().onExplodeDamage(explosion, entity);
        if (result.isAccepted()) {
            return false;
        } else if (result == ActionResult.FAIL) {
            return true;
        }

        return entity.isImmuneToExplosion(explosion);
    }
}
