package io.icker.factions.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsInputHandler;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.core.InteractionsUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ValueSettingsInputHandler.class)
public class CreateInteractionMixin {

    @Inject(at = @At("HEAD"), method = "canInteract", cancellable = true)
    private static void onBlockActivated(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        BlockHitResult hit = ItemMixin.raycast(player.world, player, RaycastContext.FluidHandling.NONE);
        Hand hand = player.getActiveHand();
        World world = player.world;
        BlockPos pos = hit.getBlockPos();
        ActionResult result = PlayerEvents.USE_BLOCK.invoker().onUseBlock(player, world, hand, hit);
        ItemStack stack = player.getStackInHand(hand);
        if(result == ActionResult.FAIL){
            InteractionsUtil.warn((ServerPlayerEntity) player, "use blocks");
            InteractionsUtil.sync(player, stack, hand);
            cir.setReturnValue(false);
        }
    }

}
