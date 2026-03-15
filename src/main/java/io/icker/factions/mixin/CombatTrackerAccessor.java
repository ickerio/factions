package io.icker.factions.mixin;

import net.minecraft.world.damagesource.CombatTracker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CombatTracker.class)
public interface CombatTrackerAccessor {
    @Accessor
    int getLastDamageTime();
}
