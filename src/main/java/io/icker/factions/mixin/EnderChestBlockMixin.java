package io.icker.factions.mixin;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.OptionalInt;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
    @Redirect(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;openHandledScreen(Lnet/minecraft/screen/NamedScreenHandlerFactory;)Ljava/util/OptionalInt;"))
    public OptionalInt openHandledScreen(PlayerEntity instance, NamedScreenHandlerFactory factory) {
        Faction faction = User.get(instance.getUuid()).getFaction();
        instance.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, playerx) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, faction.getSafe()), Text.of(String.format("%s's Safe", faction.getName()))));

        return OptionalInt.empty();
    }
}
