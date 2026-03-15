package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    @Redirect(
            method = "calculateExplodedPositions",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/ExplosionDamageCalculator;shouldBlockExplode(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)Z"))
    public boolean canDestroyBlock(
            ExplosionDamageCalculator behavior,
            Explosion explosion,
            BlockGetter world,
            BlockPos pos,
            BlockState state,
            float power) {
        InteractionResult result =
                PlayerEvents.EXPLODE_BLOCK.invoker().onExplodeBlock(explosion, world, pos, state);
        if (result.consumesAction()) {
            return true;
        } else if (result == InteractionResult.FAIL) {
            return false;
        }

        return behavior.shouldBlockExplode(explosion, world, pos, state, power);
    }

    @Redirect(
            method = "hurtEntities",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;ignoreExplosion(Lnet/minecraft/world/level/Explosion;)Z"))
    public boolean shouldDamage(Entity entity, Explosion explosion) {
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
