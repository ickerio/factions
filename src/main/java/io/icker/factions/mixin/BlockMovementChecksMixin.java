package io.icker.factions.mixin;

import com.simibubi.create.content.contraptions.BlockMovementChecks;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockMovementChecks.class)
public class BlockMovementChecksMixin {

    @Inject(at = @At("HEAD"), method = "isMovementAllowedFallback", remap = false, cancellable = true)
    private static void isMovementAllowedFallback(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        String dimension = world.getRegistryKey().getValue().toString();
        Claim claim = Claim.get(pos.getX()>>4, pos.getZ()>>4, dimension);
        if(claim == null) return;
        if(!claim.create) ci.setReturnValue(false);
    }

}
