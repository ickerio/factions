package io.icker.factions.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.icker.factions.api.events.PlayerEvents;

@Mixin(BucketItem.class)
public class BucketItemMixin {
    @Inject(method = "placeFluid", at = @At("HEAD"), cancellable = true)
    public void placeFluid(PlayerEntity player, World world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> info) {
        boolean result = PlayerEvents.USE_BUCKET.invoker().onUseBucket(player, world, pos, hitResult);
        if (!result) info.setReturnValue(!result);
    }
}