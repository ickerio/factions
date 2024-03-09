package io.icker.factions.mixin;

import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import io.github.fabricators_of_create.porting_lib.block.CustomRenderBoundingBoxBlockEntity;
import io.icker.factions.api.persistents.Claim;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicannonBlockEntity.class)
public abstract class SchematicannonMixin extends SmartBlockEntity implements NamedScreenHandlerFactory, CustomRenderBoundingBoxBlockEntity {


    @Shadow public SchematicPrinter printer;

    @Shadow public SchematicannonBlockEntity.State state;

    @Shadow public String statusMsg;

    @Shadow public boolean sendUpdate;

    public SchematicannonMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(remap = false, method = "tickPrinter", at = @At(value = "HEAD"), cancellable = true)
    public void tickPrinter(CallbackInfo ci){
        BlockPos pos = printer.getCurrentTarget();
        if(pos == null) return;
        if(this.getWorld() == null) return;
        String dimension = this.getWorld().getRegistryKey().getValue().toString();
        Claim claim = Claim.get(pos.getX()>>4, pos.getZ()>>4, dimension);
        if(claim == null) return;
        if(!claim.create) {
            this.state = SchematicannonBlockEntity.State.PAUSED;
            this.statusMsg = "notInCreateChunk";
            this.sendUpdate = true;
            ci.cancel();
        }
    }
}
