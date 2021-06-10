package io.icker.factions.event;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockInteractEvents {
    // Breaking blocks
    public static boolean breakBlocks(World world, PlayerEntity p, BlockPos pos, BlockState state, BlockEntity tile) {
        return false;
    }
    // Right clicking
    public static ActionResult useBlocks(PlayerEntity p, World world, Hand hand, BlockHitResult hitResult) {
       return ActionResult.FAIL;
    }
}