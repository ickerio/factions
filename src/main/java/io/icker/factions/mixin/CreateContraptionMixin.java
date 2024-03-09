package io.icker.factions.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.utility.Iterate;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(Contraption.class)
public class CreateContraptionMixin {

    @Shadow public boolean disassembled;

    @Shadow protected Map<BlockPos, Structure.StructureBlockInfo> blocks;

    @Inject(at = @At("HEAD"), method = "addBlocksToWorld", cancellable = true, remap = false)
    public void addBlocksToWorld(World world, StructureTransform transform, CallbackInfo ci) {
        String dimension = world.getRegistryKey().getValue().toString();
        for (boolean nonBrittles : Iterate.trueAndFalse) {
            for (Structure.StructureBlockInfo block : this.blocks.values()) {
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
