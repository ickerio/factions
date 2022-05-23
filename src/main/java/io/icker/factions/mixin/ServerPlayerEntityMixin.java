package io.icker.factions.mixin;

import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.event.FactionEvents;
import io.icker.factions.event.PlayerInteractEvents;
import io.icker.factions.util.Message;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "isInvulnerableTo", at = @At("RETURN"), cancellable = true)
    private void damage(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (damageSource.getAttacker() != null && damageSource.getAttacker().isPlayer()) {
            cir.setReturnValue(cir.getReturnValue() || PlayerInteractEvents.preventFriendlyFire(((ServerPlayerEntity) (Object) this), damageSource.getAttacker().getUuid()));
        } else {
            cir.setReturnValue(cir.getReturnValue());
        }
    }

    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    public void getPlayerListName(CallbackInfoReturnable<Text> cir) {
        Member player = Member.get(((ServerPlayerEntity)(Object) this).getUuid());
        if (player != null) {
            Faction faction = player.getFaction();
            cir.setReturnValue(new Message(String.format("[%s] ", faction.name)).format(faction.color).add(
                    new Message(((ServerPlayerEntity)(Object) this).getName().getContent().toString()).format(Formatting.WHITE)
            ).raw());
        } else {
            cir.setReturnValue(new Message("[FACTIONLESS] ").format(Formatting.GRAY).add(
                    new Message(((ServerPlayerEntity)(Object) this).getName().getContent().toString()).format(Formatting.WHITE)
            ).raw());
        }
    }
}