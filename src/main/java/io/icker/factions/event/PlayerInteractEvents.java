package io.icker.factions.event;

import io.icker.factions.config.Config;
import io.icker.factions.database.Claim;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class PlayerInteractEvents {
    public static boolean preventInteract(PlayerEntity player, World world, BlockPos pos) {

        boolean preventPlayerChunk = actionPermitted(player.getBlockPos(), world, player);
        boolean preventActionChunk = actionPermitted(pos, world, player);

        return !preventPlayerChunk || !preventActionChunk;
    }
    
    public static boolean preventUseItem(PlayerEntity player, World world) {
        return !actionPermitted(player.getBlockPos(), world, player);
    }

    public static boolean preventFriendlyFire(ServerPlayerEntity player, ServerPlayerEntity target) {
        Member playerMember = Member.get(player.getUuid());
        Member targetMember = Member.get(target.getUuid());

        if (playerMember == null || targetMember == null) return false;
        return playerMember.getFaction().name == targetMember.getFaction().name;
    }

    public static void warnPlayer(PlayerEntity target, String action) {
        new Message("Unable to %s in this claim", action)
            .format(Formatting.RED, Formatting.BOLD)
            .send(target, true);
    }

    static boolean actionPermitted(BlockPos pos, World world, PlayerEntity player) {
        //if (CHECK TOGGLE HERE) return true;

        String dimension = world.getRegistryKey().getValue().toString();
        ChunkPos actionPos = world.getChunk(pos).getPos();

        Claim claim = Claim.get(actionPos.x, actionPos.z, dimension);
        if (claim == null) return true;

        Member member = Member.get(player.getUuid());
        Faction owner = claim.getFaction();

        boolean overclaimed = owner.getClaims().size() * Config.CLAIM_WEIGHT > owner.power;
        boolean validMember = member == null ? false : member.getFaction().name == owner.name;

        return overclaimed || validMember;
    }
}