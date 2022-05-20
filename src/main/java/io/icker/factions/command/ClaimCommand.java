package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.api.persistents.Player;
import io.icker.factions.config.Config;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
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

        HashMap<String, ArrayList<Claim>> claimsMap = new HashMap<String, ArrayList<Claim>>();

        claims.forEach(claim -> {
            claimsMap.putIfAbsent(claim.level, new ArrayList<Claim>());
            claimsMap.get(claim.level).add(claim);
        });

        Message claimText = new Message("");
        claimsMap.forEach((level, array) -> {
            level = Pattern.compile("_([a-z])")
                    .matcher(level.split(":", 2)[1])
                    .replaceAll(m -> " " + m.group(1).toUpperCase());
            level = level.substring(0, 1).toUpperCase() +
                    level.substring(1);
            claimText.add("\n");
            claimText.add(new Message(level).format(Formatting.GRAY));
            claimText.filler("»");
            claimText.add(array.stream()
                    .map(claim -> String.format("(%d,%d)", claim.x, claim.z))
                    .collect(Collectors.joining(", ")));
        });

        claimText.format(Formatting.ITALIC).send(source.getPlayer(), false);
        return 1;
    }

    public static int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();

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

        String owner = existingClaim.getFaction().name == member.getFaction().name ? "Your" : "Another";
        new Message(owner + " faction already owns this chunk").fail().send(player, false);
        return 0;
    }

    public static int addCheck(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = Member.get(player.getUuid()).getFaction();

        int requiredPower = (faction.getClaims().size() + 1) * Config.CLAIM_WEIGHT;
        int maxPower = faction.getMembers().size() * Config.MEMBER_POWER + Config.BASE_POWER;

        if (maxPower >= requiredPower) {
            return add(context);
        }

        new Message("Not enough faction power to claim chunk.").fail().send(player, false);
        return 0;
    }

    public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        String dimension = world.getRegistryKey().getValue().toString();

        Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        if (existingClaim == null) {
            new Message("Cannot remove a claim on an unclaimed chunk").fail().send(player, false);
            return 0;
        }

        Faction faction = Member.get(player.getUuid()).getFaction();
        Player config = Player.get(player.getUuid());

        if (existingClaim.getFaction().name != faction.name && !config.bypass) {
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