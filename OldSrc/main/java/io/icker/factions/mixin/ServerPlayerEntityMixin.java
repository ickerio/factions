package io.icker.factions.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.icker.factions.config.Config;
import io.icker.factions.event.FactionEvents;
import io.icker.factions.event.PlayerInteractEvents;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends LivingEntity {

    protected ServerPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At("HEAD"), method = "onDeath")
    public void onDeath(DamageSource source, CallbackInfo info) {
        Entity entity = source.getSource();
        if (entity == null || !entity.isPlayer()) return;
        FactionEvents.playerDeath((ServerPlayerEntity) (Object) this);
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info) {
        if (age % Config.TICKS_FOR_POWER != 0 || age == 0) return;
        FactionEvents.powerTick((ServerPlayerEntity) (Object) this);
    }

    @Inject(at = @At("HEAD"), method = "attack", cancellable = true)
    private void attack(Entity target, CallbackInfo info) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        if (target.isPlayer() && PlayerInteractEvents.preventFriendlyFire(player, (ServerPlayerEntity) target)) {
            info.cancel();
        }

        if (!target.isLiving() && !PlayerInteractEvents.actionPermitted(target.getBlockPos(), world, player)) {
            info.cancel();
        }
    }
}