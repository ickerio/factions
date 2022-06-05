package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.icker.factions.api.events.PlayerEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "Lnet/minecraft/item/BlockItem;place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
    public void place(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> info) { 
        ActionResult result = PlayerEvents.PLACE_BLOCK.invoker().onPlaceBlock(context.getPlayer(), context.getBlockPos(), context.getWorld());
        if (result == ActionResult.FAIL) info.setReturnValue(result);
    }
}
