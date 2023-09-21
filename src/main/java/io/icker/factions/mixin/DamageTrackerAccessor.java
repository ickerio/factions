package io.icker.factions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.entity.damage.DamageTracker;

@Mixin(DamageTracker.class)
public interface DamageTrackerAccessor {
    @Accessor
    int getAgeOnLastDamage();
}
