package io.icker.factions.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import com.simibubi.create.content.schematics.requirement.ISpecialBlockEntityItemRequirement;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import com.simibubi.create.foundation.utility.IPartialSafeNBT;
import io.github.fabricators_of_create.porting_lib.block.ChunkUnloadListeningBlockEntity;
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

import javax.annotation.Nullable;

@Mixin(KineticBlockEntity.class)
public abstract class CreateKineticBlockEntityMixin extends CachedRenderBBBlockEntity
        implements IPartialSafeNBT, IInteractionChecker, ChunkUnloadListeningBlockEntity, ISpecialBlockEntityItemRequirement {

    @Shadow @Nullable public BlockPos source;

    @Shadow protected boolean overStressed;

    public CreateKineticBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(at = @At("HEAD"), method = "tick", cancellable = true, remap = false)
    public void tick(CallbackInfo ci) {
        if(source == null) return;
        if(world == null) return;
        String dimension = world.getRegistryKey().getValue().toString();
        Claim claim = Claim.get(source.getX()>>4, source.getZ()>>4, dimension);
        if(claim == null) return;
        if(!claim.create) {this.overStressed = true;}
    }
}
