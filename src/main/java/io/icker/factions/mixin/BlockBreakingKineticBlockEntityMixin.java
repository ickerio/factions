package io.icker.factions.mixin;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBreakingKineticBlockEntity.class)
public abstract class BlockBreakingKineticBlockEntityMixin extends KineticBlockEntity {

    @Shadow protected BlockPos breakingPos;

    public BlockBreakingKineticBlockEntityMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Shadow protected abstract BlockPos getBreakingPos();

    @Inject(at = @At("HEAD"), method = "tick", remap = false, cancellable = true)
    public void tick(CallbackInfo ci){
        this.breakingPos = getBreakingPos();
        if(this.world == null) return;
        if(this.world.isClient) return;
        String dimension = this.world.getRegistryKey().getValue().toString();
        Claim claim = Claim.get(breakingPos.getX()>>4, breakingPos.getZ()>>4, dimension);
        if(claim == null) return;
        if(!claim.create) this.overStressed = true;
    }

}
