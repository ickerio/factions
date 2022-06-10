package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.config.Config;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.OptionalInt;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
    @Redirect(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;openHandledScreen(Lnet/minecraft/screen/NamedScreenHandlerFactory;)Ljava/util/OptionalInt;"))
    public OptionalInt openHandledScreen(PlayerEntity instance, NamedScreenHandlerFactory factory) {
        if (FactionsMod.CONFIG.FACTION_SAFE == Config.SafeOptions.ENDERCHEST || FactionsMod.CONFIG.FACTION_SAFE == Config.SafeOptions.ON) {
            PlayerEvents.OPEN_SAFE.invoker().onOpenSafe(instance);
        } // TODO: open normal ender chest

        return OptionalInt.empty();
    }
}
