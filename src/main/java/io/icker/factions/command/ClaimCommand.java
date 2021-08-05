package io.icker.factions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Claim;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ClaimCommand {
	public static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		ArrayList<Claim> claims = Member.get(player.getUuid()).getFaction().getClaims();
		int count = claims.size();

		new Message("You have ")
			.add(new Message(String.valueOf(count)).format(Formatting.YELLOW))
			.add(" claim%s", count == 1 ? "" : "s")
			.send(source.getPlayer(), false);

		if (count == 0) return 1;

		String claimText = claims.stream() // TODO: show dimension
			.map(claim -> String.format("(%d, %d)", claim.x, claim.z))
			.collect(Collectors.joining(", "));

		new Message(claimText).format(Formatting.ITALIC).send(source.getPlayer(), false);
		return 1;
	}

	public static int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = player.getServerWorld();
		
		ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
		String dimension = world.getRegistryKey().getValue().toString();

		Member member = Member.get(player.getUuid());
		Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);
		
		if (existingClaim == null) {
			Faction faction = member.getFaction();
			faction.addClaim(chunkPos.x, chunkPos.z, dimension);
			new Message("%s claimed chunk (%d, %d)", player.getName().asString(), chunkPos.x, chunkPos.z).send(faction);
			return 1;
		}
		
		String owner = existingClaim.getFaction().name.equals(member.getFaction().name) ? "Your" : "Another";
		new Message(owner + " faction already owns this chunk").fail().send(player, false);
		return 0;
	}

	public static int square(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = player.getServerWorld();

		ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
		String dimension = world.getRegistryKey().getValue().toString();

		Member member = Member.get(player.getUuid());
		Faction faction = member.getFaction();

		int size = IntegerArgumentType.getInteger(context, "size");
		int claimed = 0;
		for (int xOffset = -size; xOffset < size; xOffset++) {
			for (int zOffset = -size; zOffset < size; zOffset++) {
				Claim existingClaim = Claim.get(chunkPos.x + xOffset, chunkPos.z + zOffset, dimension);
				if (existingClaim == null) {
					faction.addClaim(chunkPos.x + xOffset, chunkPos.z + zOffset, dimension);
					claimed++;
				} else {
					String owner = existingClaim.getFaction().name.equals(member.getFaction().name) ? "Your" : "Another";
					new Message("%s faction already owns the chunk at (%d, %d)", owner, chunkPos.x + xOffset, chunkPos.z + zOffset).fail().send(player, false);
				}

			}
		}

		if (claimed > 0) {
			new Message("%s claimed %s chunk%s near (%d, %d)", player.getName().asString(), claimed, claimed == 1 ? "" : "s", chunkPos.x, chunkPos.z).send(faction);
			return 1;
		}

		return 0;
	}

	public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = player.getServerWorld();

		ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
		String dimension = world.getRegistryKey().getValue().toString();

		Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

		if (existingClaim == null) {
			new Message("Cannot remove a claim on an unclaimed chunk").fail().send(player, false);
			return 0;
		}

		Faction faction = Member.get(player.getUuid()).getFaction();
		if (existingClaim.getFaction().name != faction.name) {
			new Message("Cannot remove a claim owned by another faction").fail().send(player, false);
			return 0;
		}

		existingClaim.remove();
		new Message("%s removed claim at chunk (%d, %d)", player.getName().asString(), existingClaim.x, existingClaim.z).send(faction);
		return 1;
	}

	public static int removeAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Faction faction = Member.get(player.getUuid()).getFaction();

		faction.removeAllClaims();
		new Message("%s removed all claims", player.getName().asString()).send(faction);
		return 1;
	}
}