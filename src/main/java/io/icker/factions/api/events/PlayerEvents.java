package io.icker.factions.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PlayerEvents {
    public static final Event<BreakBlock> BREAK_BLOCK = EventFactory.createArrayBacked(BreakBlock.class, callbacks -> (player, position, world) -> {
        for (BreakBlock callback : callbacks) {
            ActionResult result = callback.onBreakBlock(player, position, world);
 
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    public static final Event<UseBlock> USE_BLOCK = EventFactory.createArrayBacked(UseBlock.class, callbacks -> (player, world, hand, hitResult) -> {
        for (UseBlock callback : callbacks) {
            ActionResult result = callback.onUseBlock(player, world, hand, hitResult);
 
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    public static final Event<UseItem> USE_ITEM = EventFactory.createArrayBacked(UseItem.class, callbacks -> (player, world, stack, hand) -> {
        for (UseItem callback : callbacks) {
            ActionResult result = callback.onUseItem(player, world, stack, hand);
 
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    public static final Event<IsInvulnerable> IS_INVULNERABLE = EventFactory.createArrayBacked(IsInvulnerable.class, callbacks -> (source, target) -> {
        for (IsInvulnerable callback : callbacks) {
            if (callback.isInvulnerable(source, target)) {
                return true;
            }
        }
        return false;
    });

    public static final Event<OnMove> ON_MOVE = EventFactory.createArrayBacked(OnMove.class, callbacks -> (player) -> {
        for (OnMove callback : callbacks) {
            callback.onMove(player);
        }
    });


    @FunctionalInterface
    public interface BreakBlock {
		ActionResult onBreakBlock(PlayerEntity player, BlockPos position, World world);
    }

    @FunctionalInterface
    public interface UseBlock {
		ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult);
    }

    @FunctionalInterface
    public interface UseItem {
		ActionResult onUseItem(PlayerEntity player, World world, ItemStack stack, Hand hand);
    }

    @FunctionalInterface
    public interface IsInvulnerable {
		boolean isInvulnerable(Entity source, Entity target);
    }

    @FunctionalInterface
    public interface OnMove {
        void onMove(ServerPlayerEntity player);
    }
}
