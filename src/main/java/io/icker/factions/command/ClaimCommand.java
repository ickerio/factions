package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.config.Config;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClaimCommand implements Command {
    private int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        List<Claim> claims = User.get(player.getUuid()).getFaction().getClaims();
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

    private int addForced(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        String dimension = world.getRegistryKey().getValue().toString();

        User user = User.get(player.getUuid());
        Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        if (existingClaim != null) {
            String owner = existingClaim.getFaction().getID() == user.getFaction().getID() ? "Your" : "Another";
            new Message(owner + " faction already owns this chunk").fail().send(player, false);
            return 0;
        }

        Faction faction = user.getFaction();
        faction.addClaim(chunkPos.x, chunkPos.z, dimension);
        new Message("Chunk (%d, %d) claimed by %s ", chunkPos.x, chunkPos.z, player.getName().asString()).send(faction);
        return 1;
    }

    private int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = User.get(player.getUuid()).getFaction();

        int requiredPower = (faction.getClaims().size() + 1) * Config.CLAIM_WEIGHT;
        int maxPower = faction.getUsers().size() * Config.MEMBER_POWER + Config.BASE_POWER;

        if (maxPower < requiredPower) {
            new Message("Not enough faction power to claim chunk.").fail().send(player, false);
            return 0;
        }

        return addForced(context);
    }

    private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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

        User user = User.get(player.getUuid());
        Faction faction = user.getFaction();

        if (!user.isBypassOn() && existingClaim.getFaction().getID() != faction.getID()) {
            new Message("Cannot remove a claim owned by another faction").fail().send(player, false);
            return 0;
        }

        existingClaim.remove();
        new Message("Claim (%d, %d) removed by %s", existingClaim.x, existingClaim.z, player.getName().asString()).send(faction);
        return 1;
    }

    private int removeAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = User.get(player.getUuid()).getFaction();

        faction.removeAllClaims();
        new Message("All claims removed by %s", player.getName().asString()).send(faction);
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("claim")
            .requires(Requires.hasPerms("factions.claim", 0))
            .requires(Requires.isCommander())
            .then(
                CommandManager.literal("add")
                .requires(Requires.hasPerms("factions.claim.add", 0))
                .then(CommandManager.literal("force").executes(this::addForced))
                .executes(this::add)
            )
            .then(
                CommandManager.literal("list")
                .requires(Requires.hasPerms("factions.claim.list", 0))
                .executes(this::list)
            )
            .then(
                CommandManager.literal("remove")
                .requires(Requires.hasPerms("factions.claim.remove", 0))
                .then(
                    CommandManager.literal("all")
                    .requires(Requires.hasPerms("factions.claim.remove.all", 0))
                    .executes(this::removeAll)
                )
                .executes(this::remove)
            )
            .build();
    }
}