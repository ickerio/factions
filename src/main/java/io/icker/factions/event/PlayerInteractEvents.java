package io.icker.factions.event;

import io.icker.factions.database.Claim;
import io.icker.factions.database.Member;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class PlayerInteractEvents {
    public static boolean breakBlocks(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity tile) {
        Member member = Member.get(player.getUuid());

        boolean canPlayerChunk = actionPermitted(player.getBlockPos(), world, member);
        boolean canActionChunk = actionPermitted(pos, world, member);

        return canPlayerChunk && canActionChunk;
    }

    public static ActionResult useBlocks(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        Member member = Member.get(player.getUuid());

        boolean canPlayerChunk = actionPermitted(player.getBlockPos(), world, member);
        boolean canActionChunk = actionPermitted(hitResult.getBlockPos(), world, member);

        if (canPlayerChunk && canActionChunk) 
            return ActionResult.PASS;
        else
            return ActionResult.FAIL;
    }

    public static TypedActionResult<ItemStack> useItem(PlayerEntity player, World world, Hand hand) {
        Member member = Member.get(player.getUuid());

        if (actionPermitted(player.getBlockPos(), world, member))
            return ActionResult.PASS;
        else
            return ActionResult.FAIL;
    }

    public static boolean actionPermitted(BlockPos pos, World world, Member member) {
        String dimension = world.getRegistryKey().getValue().toString();
        ChunkPos actionPos =  world.getChunk(pos).getPos();

        Claim claim = Claim.get(actionPos.x, actionPos.z, dimension);
        return claim == null || (member == null ? false : member.getFaction().name == claim.getFaction().name);
    }
}