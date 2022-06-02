package io.icker.factions.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.icker.factions.core.PlayerInteractions;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow
    public ServerWorld world;
    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "tryBreakBlock", cancellable = true)
    private void tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        if (!PlayerInteractions.actionPermitted(pos, world, player)) {
            PlayerInteractions.warnPlayer(player, "break blocks");
            info.setReturnValue(false);
        }
    }

    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    public void interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> info) {
        if (PlayerInteractions.preventInteract(player, world, blockHitResult)) {
            PlayerInteractions.warnPlayer(player, "use blocks");
            PlayerInteractions.syncItem(player, stack, hand);
            info.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
    public void interactItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        if (PlayerInteractions.preventUseItem(player, world, stack)) {
            PlayerInteractions.warnPlayer(player, "use items");
            PlayerInteractions.syncItem(player, stack, hand);
            info.setReturnValue(ActionResult.FAIL);
        }
    }
}