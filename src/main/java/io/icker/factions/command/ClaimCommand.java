package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Claim;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

// TODO: rewrite given that Claim.add will return null on duplicate add

public class ClaimCommand {
	public static int claim(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		ServerPlayerEntity player = source.getPlayer();
		ServerWorld world = player.getServerWorld();
		
		ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
		String dimension = world.getRegistryKey().getValue().toString();

		Member member = Member.get(player.getUuid());
		Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);
		
		if (existingClaim == null) {
			member.getFaction().claim(chunkPos.x, chunkPos.z, dimension);
			source.sendFeedback(new TranslatableText("factions.command.claim.success", chunkPos.x, chunkPos.z).formatted(Formatting.GREEN), false);
			return 1;
		}
		
		if (existingClaim.getFaction().name == member.getFaction().name)
			source.sendFeedback(new TranslatableText("factions.command.claim.already_owns").formatted(Formatting.RED), false);
		else 
			source.sendFeedback(new TranslatableText("factions.command.claim.already_claimed").formatted(Formatting.RED), false);
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
			source.sendFeedback(new TranslatableText("factions.command.claim_remove.unclaimed").formatted(Formatting.RED), false);
			return 0;
		}

		Faction faction = Member.get(player.getUuid()).getFaction();
		if (existingClaim.getFaction().name != faction.name) {
			source.sendFeedback(new TranslatableText("factions.command.claim_remove.not_yours").formatted(Formatting.RED), false);
			return 0;
		}

		existingClaim.remove();
		source.sendFeedback(new TranslatableText("factions.command.claim_remove.success").formatted(Formatting.GREEN), false);
		return 1;
	}
}