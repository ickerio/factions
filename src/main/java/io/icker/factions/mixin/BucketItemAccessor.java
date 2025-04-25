package io.icker.factions.mixin;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BucketItem.class)
public interface BucketItemAccessor {
    @Accessor
    Fluid getFluid();
}
