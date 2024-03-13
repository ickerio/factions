package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.core.InteractionsUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemActionResultMixin {

    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    public void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir){
        BlockHitResult hit = ItemMixin.raycast(world, user, RaycastContext.FluidHandling.NONE);
        ActionResult result = PlayerEvents.USE_BLOCK.invoker().onUseBlock(user, world, hand, hit);
        ItemStack stack = user.getStackInHand(hand);
        if(result == ActionResult.FAIL){
            InteractionsUtil.warn((ServerPlayerEntity) user, "use blocks");
            InteractionsUtil.sync(user, stack, hand);
            cir.setReturnValue(TypedActionResult.fail(stack));
        }
    }

}
