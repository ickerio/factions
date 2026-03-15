package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseContainerBlockEntity.class)
public class BaseContainerBlockEntityMixin {
    @Inject(
            method = "canOpen(Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("RETURN"),
            cancellable = true)
    private void checkUnlocked(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(
                cir.getReturnValue()
                        && PlayerEvents.USE_INVENTORY
                                        .invoker()
                                        .onUseInventory(
                                                player,
                                                ((BaseContainerBlockEntity) (Object) this)
                                                        .getBlockPos(),
                                                ((BaseContainerBlockEntity) (Object) this)
                                                        .getLevel())
                                != InteractionResult.FAIL);
    }
}
