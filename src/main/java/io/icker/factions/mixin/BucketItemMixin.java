package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.item.BucketItem;
import net.minecraft.fluid.Fluid;

@Mixin(BucketItem.class)
public interface BucketItemMixin {
    @Accessor
    Fluid getFluid();
}