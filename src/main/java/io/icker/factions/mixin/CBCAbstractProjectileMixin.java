package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.config.MunitionProperties;

import java.util.Date;

@Mixin(AbstractCannonProjectile.class)
public abstract class CBCAbstractProjectileMixin {

    @Inject(at = @At("HEAD"), method = "onImpact", remap = false, cancellable = true)
    protected void onImpact(HitResult result, boolean stopped, CallbackInfo ci) {
        Vec3d pos = result.getPos();
        ChunkPos cp = new ChunkPos((int)pos.x>>4, (int)pos.z>>4);
        String dimension = ((AbstractCannonProjectile)(Object)this).world.getRegistryKey().getValue().toString();
        Claim claim = Claim.get(cp.x, cp.z, dimension);
        if (claim == null) return;
        boolean notInWar = claim.getFaction().getEnemiesWith().isEmpty();
        if (notInWar) {
            ((AbstractCannonProjectile)(Object)this).discard();
            ci.cancel();
            return;
        }
        int hours = new Date().getHours();
        boolean isCancerTime = hours > FactionsMod.CONFIG.OFFWAR_HOURS_START && hours < FactionsMod.CONFIG.OFFWAR_HOURS_END;
        if (isCancerTime) {
            ((AbstractCannonProjectile)(Object)this).discard();
            ci.cancel();
            return;
        }
        if (claim.getFaction().isAdmin()) {
            ((AbstractCannonProjectile)(Object)this).discard();
            ci.cancel();
            return;
        }
    }

    @Inject(at = @At("HEAD"), method = "clipAndDamage", cancellable = true, remap = false)
    public void clip(CallbackInfo ci){
        Vec3d pos = ((AbstractCannonProjectile)(Object)this).getPos();
        ChunkPos cp = new ChunkPos((int)pos.x>>4, (int)pos.z>>4);
        String dimension = ((AbstractCannonProjectile)(Object)this).world.getRegistryKey().getValue().toString();
        Claim claim = Claim.get(cp.x, cp.z, dimension);
        if (claim == null) return;
        boolean notInWar = claim.getFaction().getEnemiesWith().isEmpty();
        if (notInWar) {
            ((AbstractCannonProjectile)(Object)this).discard();
            ci.cancel();
            return;
        }
        int hours = new Date().getHours();
        boolean isCancerTime = hours > FactionsMod.CONFIG.OFFWAR_HOURS_START && hours < FactionsMod.CONFIG.OFFWAR_HOURS_END;
        if (isCancerTime) {
            ((AbstractCannonProjectile)(Object)this).discard();
            ci.cancel();
            return;
        }
        if (claim.getFaction().isAdmin()) {
            ((AbstractCannonProjectile)(Object)this).discard();
            ci.cancel();
            return;
        }


    }
}
