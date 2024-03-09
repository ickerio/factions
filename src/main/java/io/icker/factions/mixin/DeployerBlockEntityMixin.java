package io.icker.factions.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemTransferable;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

@Mixin(DeployerBlockEntity.class)
public abstract class DeployerBlockEntityMixin extends KineticBlockEntity implements ItemTransferable {

    public DeployerBlockEntityMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Shadow protected abstract Vec3d getMovementVector();


    @Inject(at = @At("HEAD"), method = "activate", cancellable = true, remap = false)
    protected void activate(CallbackInfo ci) {
        Vec3d movementVector = this.getMovementVector();
        Direction direction = this.getCachedState().get(FACING);
        Vec3d center = VecHelper.getCenterOf(pos);
        BlockPos clickedPos = pos.offset(direction, 2);
        if(world == null) return;
        String dimension = world.getRegistryKey().getValue().toString();
        Claim claim = Claim.get(clickedPos.getX()>>4, clickedPos.getZ()>>4, dimension);
        if(claim == null) return;
        if(!claim.create) ci.cancel();
    }

}
