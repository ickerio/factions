package io.icker.factions.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.utility.Iterate;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.structure.Structure;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(AbstractContraptionEntity.class)
public abstract class CreateAbstractContraptionMixin {
    @Shadow protected Contraption contraption;

    @Shadow protected abstract void onContraptionStalled();

    @Shadow public abstract Contraption getContraption();

    @Shadow public abstract void disassemble();

    @Shadow protected abstract StructureTransform makeStructureTransform();

    @Shadow public abstract boolean isAliveOrStale();

    @Inject(at = @At("HEAD"), method = "disassemble", remap = false, cancellable = true)
    public void tick(CallbackInfo ci) {

        if (!this.isAliveOrStale())
            return;
        if (contraption == null)
            return;
        String dimension = this.contraption.getContraptionWorld().getLevel().getRegistryKey().getValue().toString();
        StructureTransform transform = this.makeStructureTransform();
        for (boolean nonBrittles : Iterate.trueAndFalse) {
            for (Structure.StructureBlockInfo block : this.contraption.getBlocks().values()) {
                BlockPos targetPos = transform.apply(block.pos);
                Claim claim = Claim.get(targetPos.getX()>>4, targetPos.getZ()>>4, dimension);
                if(claim == null) continue;
                if(!claim.create) {
                    ci.cancel();
                }
            }
        }

    }
}
