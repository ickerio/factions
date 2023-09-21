package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.icker.factions.api.events.PlayerEvents;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

@Mixin(LockableContainerBlockEntity.class)
public class LockableContainerBlockEntityMixin {
    @Inject(method = "checkUnlocked(Lnet/minecraft/entity/player/PlayerEntity;)Z",
            at = @At("RETURN"), cancellable = true)
    private void checkUnlocked(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(
                cir.getReturnValue() && PlayerEvents.USE_INVENTORY.invoker().onUseInventory(player,
                        ((LockableContainerBlockEntity) (Object) this).getPos(),
                        ((LockableContainerBlockEntity) (Object) this)
                                .getWorld()) != ActionResult.FAIL);
    }
}
