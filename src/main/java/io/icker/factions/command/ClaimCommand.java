package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Claim;
import io.icker.factions.database.Database;
import io.icker.factions.database.Member;
import io.icker.factions.util.FactionsUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

public class ClaimCommand {

	public static int claim(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		ChunkPos chunkPos = getPosFromPlayer(player);

		Member member = Database.Members.get(player.getUuid());
		Claim existingClaim = Database.Claims.get(chunkPos.x, chunkPos.z, "Overworld");

		// TODO: prevent someone not in team from running this command
		
		if (existingClaim == null) {
			member.getFaction().claim(chunkPos.x, chunkPos.z, "Overworld"); // TODO: dimension
			FactionsUtil.Message.sendSuccess(player, "Success! Chunk claimed at %s,%s".formatted(chunkPos.x, chunkPos.z));
			return 1;
		} else if (existingClaim.getFaction().name == member.getFaction().name) {
			FactionsUtil.Message.sendError(player, "Your faction already owns this chunk");
			return 0;
		} else {
			FactionsUtil.Message.sendError(player, "This chunk is already claimed by %s".formatted(existingClaim.getFaction().name));
			return 0;
		}
	}

	public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		ChunkPos chunkPos = getPosFromPlayer(player);

		Member member = Database.Members.get(player.getUuid());
		Claim existingClaim = Database.Claims.get(chunkPos.x, chunkPos.z, "Overworld");

		if (existingClaim == null) {
			FactionsUtil.Message.sendError(player, "Cannot unclaim an unclaimed chunk");
			return 0;
		} else if (existingClaim.getFaction().name != member.getFaction().name) {
			FactionsUtil.Message.sendError(player, "Cannot unclaim a chunk your faction doesn't own");
			return 0;
		} else {
			member.getFaction().removeClaim(chunkPos.x, chunkPos.z, "Overworld");
			FactionsUtil.Message.sendSuccess(player, "Success! Chunk claim removed at %s,%s".formatted(chunkPos.x, chunkPos.z));
			return 1;
		}
	}

	static ChunkPos getPosFromPlayer(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();

		Chunk chunk = world.getChunk(player.getBlockPos());
		return chunk.getPos();
	}
}