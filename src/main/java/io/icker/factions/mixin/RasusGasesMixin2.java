package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.thetarasus.rasusgases.gas_bomb.GasBombEntity;

import java.util.Date;

@Mixin(GasBombEntity.class)
public abstract class RasusGasesMixin2 extends Entity {

    public RasusGasesMixin2(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At("HEAD"), remap = false, method = "explode", cancellable = true)
    private void explode(CallbackInfo ci) {
        Date date = new Date();
        boolean cancerTime = date.getHours() > FactionsMod.CONFIG.OFFWAR_HOURS_START && date.getHours() < FactionsMod.CONFIG.OFFWAR_HOURS_END;
        if(cancerTime) {
            this.discard();
            ci.cancel();
        }
        BlockPos pos = this.getBlockPos();
        String dimension = world.getRegistryKey().getValue().toString();
        Claim claim = Claim.get(pos.getX() >> 4, pos.getZ() >> 4, dimension);
        if (claim == null) return;
        boolean notInWar = claim.getFaction().getEnemiesWith().isEmpty();
        if (notInWar) {
            this.discard();
            ci.cancel();
        }
        if (claim.getFaction().isAdmin()) {
            this.discard();
            ci.cancel();
        }

    }
}
