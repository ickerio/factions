package io.icker.factions.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PlayerEvents {
    public static final Event<IsInvulnerable> IS_INVULNERABLE = EventFactory.createArrayBacked(IsInvulnerable.class, callbacks -> (source, target) -> {
        for (IsInvulnerable callback : callbacks) {
            if (callback.isInvulnerable(source, target)) {
                return true;
            }
        }
        return false;
    });

    public static final Event<PlaceBlock> PLACE_BLOCK = EventFactory.createArrayBacked(PlaceBlock.class, callbacks -> (player, position, world) -> {
        for (PlaceBlock callback : callbacks) {
            ActionResult result = callback.onPlaceBlock(player, position, world);
 
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    public static final Event<UseBucket> USE_BUCKET = EventFactory.createArrayBacked(UseBucket.class, callbacks -> (player, world, pos, hitResult) -> {
        for (UseBucket callback : callbacks) {
            if (callback.onUseBucket(player, world, pos, hitResult)) {
                return true;
            }
        }
        return false;
    });

    @FunctionalInterface
    public interface IsInvulnerable {
		boolean isInvulnerable(Entity source, Entity target);
    }

    @FunctionalInterface
    public interface PlaceBlock {
		ActionResult onPlaceBlock(PlayerEntity player, BlockPos position, World world);
    }

    @FunctionalInterface
    public interface UseBucket {
		boolean onUseBucket(PlayerEntity player, World world, BlockPos pos, BlockHitResult hitResult);
    }
}
