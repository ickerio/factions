package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.core.InteractionsUtil;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow
    public ServerWorld world;
    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "tryBreakBlock", cancellable = true)
    private void tryBreakBlock(BlockPos position, CallbackInfoReturnable<Boolean> info) {
        ActionResult result = PlayerEvents.BREAK_BLOCK.invoker().onBreakBlock(player, position, world);

        if (result == ActionResult.FAIL) {
            InteractionsUtil.warn(player, "break blocks");
            info.setReturnValue(false);
        }
    }

    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    public void interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> info) {
        ActionResult result = PlayerEvents.USE_BLOCK.invoker().onUseBlock(player, world, hand, hitResult);

        if (result == ActionResult.FAIL) {
            InteractionsUtil.warn(player, "use blocks");
            InteractionsUtil.sync(player, stack, hand);
            info.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
    public void interactItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        ActionResult result = PlayerEvents.USE_ITEM.invoker().onUseItem(player, world, stack, hand);

        if (result == ActionResult.FAIL) {
            InteractionsUtil.warn(player, "use items");
            InteractionsUtil.sync(player, stack, hand);
            info.setReturnValue(ActionResult.FAIL);
        }
    }
}