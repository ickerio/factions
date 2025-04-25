package io.icker.factions.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Item.class)
public interface ItemInvoker {
    @Invoker("raycast")
    static BlockHitResult raycast(
            World world, PlayerEntity player, RaycastContext.FluidHandling fluidHandling) {
        throw new AssertionError();
    }
}
