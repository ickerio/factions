package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Date;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ExplosionMixin extends World {


    protected ExplosionMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    @Shadow public abstract ServerWorld toServerWorld();

    @Inject(at = @At("HEAD"), method = "createExplosion", cancellable = true)
    public void createExplosion(Entity entity, DamageSource damageSource, ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType destructionType, CallbackInfoReturnable<Explosion> cir) {
        int hours = new Date().getHours();
        boolean isCancerTime = hours > FactionsMod.CONFIG.OFFWAR_HOURS_START && hours < FactionsMod.CONFIG.OFFWAR_HOURS_END;
        if (isCancerTime) {
            cir.setReturnValue(new Explosion(this, entity, x, y, z, 0, false, Explosion.DestructionType.NONE));
            return;
        }
        Vec3d pos = new Vec3d(x, y, z);
        String dimension = this.toServerWorld().getRegistryKey().getValue().toString();
        Claim claim = Claim.get((int) x >> 4, (int) z >> 4, dimension);
        if (claim == null) return;
        boolean notInWar = claim.getFaction().getEnemiesWith().isEmpty();
        if (notInWar) {
            cir.setReturnValue(new Explosion(this, entity, x, y, z, 0, false, Explosion.DestructionType.NONE));
            return;
        }
        if (claim.getFaction().isAdmin()) {
            cir.setReturnValue(new Explosion(this, entity, x, y, z, 0, false, Explosion.DestructionType.NONE));
            return;
        }
    }
}
