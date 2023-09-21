package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.icker.factions.api.events.PlayerEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Redirect(method = "interactBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;"))
    public ActionResult place(ItemStack instance, ItemUsageContext context) {
        if (PlayerEvents.PLACE_BLOCK.invoker().onPlaceBlock(context) == ActionResult.FAIL) {
            return ActionResult.FAIL;
        }
        return instance.useOnBlock(context);
    }
}
