package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.User;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    public void onUse(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> info) {
        if (FactionsMod.CONFIG.SAFE == null || !FactionsMod.CONFIG.SAFE.ENDER_CHEST) return;

        InteractionResult result =
                PlayerEvents.OPEN_SAFE
                        .invoker()
                        .onOpenSafe(player, User.get(player.getUUID()).getFaction());
        if (result != InteractionResult.PASS) {
            info.setReturnValue(result);
        }
    }
}
