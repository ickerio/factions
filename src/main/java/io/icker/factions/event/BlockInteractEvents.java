package io.icker.factions.event;

import io.icker.factions.teams.Claim;
import io.icker.factions.teams.Database;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class BlockInteractEvents {
    // Breaking blocks
    public static boolean breakBlocks(World world, PlayerEntity p, BlockPos pos, BlockState state, BlockEntity tile) {
		Chunk chunk = world.getChunk(pos);
		ChunkPos chunkPos =  chunk.getPos();

        Claim claim = Database.Claims.get(chunkPos.x, chunkPos.z, "Overworld");
        if (claim == null || Database.Members.get(p.getUuid()).getTeam().name == claim.getTeam().name) {
            return true;
        } else {
            return false;
        }
    }
    // Right clicking
    public static ActionResult useBlocks(PlayerEntity p, World world, Hand hand, BlockHitResult hitResult) {
       return ActionResult.FAIL;
    }
}