package io.icker.factions.event;

import io.icker.factions.database.Claim;
import io.icker.factions.database.Member;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class PlayerInteractEvents {
    public static boolean preventBlockChange(PlayerEntity player, World world, BlockPos pos) {
        Member member = Member.get(player.getUuid());

        boolean preventPlayerChunk = actionPermitted(player.getBlockPos(), world, member);
        boolean preventActionChunk = actionPermitted(pos, world, member);

        return !preventPlayerChunk || !preventActionChunk;
    }
    
    public static boolean preventUseItem(PlayerEntity player, World world) {
        Member member = Member.get(player.getUuid());
        return !actionPermitted(player.getBlockPos(), world, member);
    }

    public static void warnPlayer(PlayerEntity target, String action) {
        target.sendMessage(new LiteralText(String.format("Unable to %s in this claim", action))
            .formatted(Formatting.RED, Formatting.BOLD), true);
    }

    static boolean actionPermitted(BlockPos pos, World world, Member member) {
        String dimension = world.getRegistryKey().getValue().toString();
        ChunkPos actionPos =  world.getChunk(pos).getPos();

        Claim claim = Claim.get(actionPos.x, actionPos.z, dimension);

        return claim == null || (member == null ? false : member.getFaction().name == claim.getFaction().name);
    }
}