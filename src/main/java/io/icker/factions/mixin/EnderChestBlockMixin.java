package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.User;
import net.minecraft.block.BlockState;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    public void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit, CallbackInfoReturnable<ActionResult> info) {
        if (FactionsMod.CONFIG.SAFE == null || !FactionsMod.CONFIG.SAFE.ENDER_CHEST)
            return;

        ActionResult result = PlayerEvents.OPEN_SAFE.invoker().onOpenSafe(player,
                User.get(player.getUuid()).getFaction());
        if (result != ActionResult.PASS) {
            info.setReturnValue(result);
        }
    }
}
