package io.icker.factions.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsInputHandler;
import com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction.EdgeInteractionHandler;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.core.InteractionsUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EdgeInteractionHandler.class)
public class CreateInteractionMixin1 {

    @Inject(at = @At("HEAD"), method = "onBlockActivated", cancellable = true)
    private static void onBlockActivated(PlayerEntity player, World world, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        BlockPos pos = hit.getBlockPos();
        ActionResult result = PlayerEvents.USE_BLOCK.invoker().onUseBlock(player, world, hand, hit);
        ItemStack stack = player.getStackInHand(hand);
        if(result == ActionResult.FAIL){
            InteractionsUtil.warn((ServerPlayerEntity) player, "use blocks");
            InteractionsUtil.sync(player, stack, hand);
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

}
