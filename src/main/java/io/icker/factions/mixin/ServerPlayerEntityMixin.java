package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.core.FactionsManager;
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
        FactionsManager.playerDeath((ServerPlayerEntity) (Object) this);
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info) {
        if (age % FactionsMod.CONFIG.TICKS_FOR_POWER != 0 || age == 0) return;
        FactionsManager.powerTick((ServerPlayerEntity) (Object) this);
    }

    @Inject(method = "isInvulnerableTo", at = @At("RETURN"), cancellable = true)
    private void isInvulnerableTo(DamageSource damageSource, CallbackInfoReturnable<Boolean> info) {
        Entity source = damageSource.getAttacker();
        if (source == null) return;
        boolean result = PlayerEvents.IS_INVULNERABLE.invoker().isInvulnerable(damageSource.getAttacker(), (ServerPlayerEntity) (Object) this);
        if (result) info.setReturnValue(result);
    }

    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    public void getPlayerListName(CallbackInfoReturnable<Text> cir) {
        User member = User.get(((ServerPlayerEntity)(Object) this).getUuid());
        if (member.isInFaction()) {
            Faction faction = member.getFaction();
            cir.setReturnValue(new Message(String.format("[%s] ", faction.getName())).format(faction.getColor()).add(
                    new Message(((ServerPlayerEntity)(Object) this).getName().asString()).format(Formatting.WHITE)
            ).raw());
        } else {
            cir.setReturnValue(new Message("[FACTIONLESS] ").format(Formatting.GRAY).add(
                    new Message(((ServerPlayerEntity)(Object) this).getName().asString()).format(Formatting.WHITE)
            ).raw());
        }
    }
}