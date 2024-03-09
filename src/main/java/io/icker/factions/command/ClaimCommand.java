package io.icker.factions.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

public class ClaimCommand implements Command {
    private int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        List<Claim> claims = User.get(player.getName().getString()).getFaction().getClaims();
        int count = claims.size();

        new Message("You have ")
                .add(new Message(String.valueOf(count)).format(Formatting.YELLOW))
                .add(" claim%s", count == 1 ? "" : "s")
                .send(source.getPlayer(), false);

        if (count == 0) return 1;

        HashMap<String, ArrayList<Claim>> claimsMap = new HashMap<>();

        claims.forEach(claim -> {
            claimsMap.putIfAbsent(claim.level, new ArrayList<>());
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

    private int addForced(CommandContext<ServerCommandSource> context, int size) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();

        Faction faction = User.get(player.getName().getString()).getFaction();
        String dimension = world.getRegistryKey().getValue().toString();
        ArrayList<ChunkPos> chunks = new ArrayList<>();

        for (int x = -size + 1; x < size; x++) {
            for (int y = -size + 1; y < size; y++) {
                ChunkPos chunkPos = world.getChunk(player.getBlockPos().add(x << 4, 0, y << 4)).getPos();
                Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

                if (existingClaim != null) {
                    if (size == 1) {
                        String owner = existingClaim.getFaction().getID() == faction.getID() ? "Your" : "Another";
                        new Message(owner + " faction already owns this chunk").fail().send(player, false);
                        return 0;
                    } else if (existingClaim.getFaction().getID() != faction.getID()) {
                        new Message("Another faction already owns a chunk").fail().send(player, false);
                        return 0;
                    }
                }

                chunks.add(chunkPos);
            }
        }

        chunks.forEach(chunk -> faction.addClaim(chunk.x, chunk.z, dimension));
        if (size == 1) {
            new Message("Chunk (%d, %d) claimed by %s", chunks.get(0).x, chunks.get(0).z, player.getName().getString())
                .send(faction);
        } else {
            new Message("Chunks (%d, %d) to (%d, %d) claimed by %s", chunks.get(0).x, chunks.get(0).z,
                    chunks.get(0).x + size - 1, chunks.get(0).z + size - 1, player.getName().getString())
                .send(faction);
        }

        return 1;
    }

    private int addForcedOutpost(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();

        Faction faction = User.get(player.getName().getString()).getFaction();
        String dimension = world.getRegistryKey().getValue().toString();
        ArrayList<ChunkPos> chunks = new ArrayList<>();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        int outpostName = faction.homesLength();
        if (existingClaim != null) {
            if (existingClaim.getFaction().getID() != faction.getID()) {
                new Message("Another faction already owns a chunk").fail().send(player, false);
                return 0;
            }
            String owner = existingClaim.getFaction().getID() == faction.getID() ? "Your" : "Another";
            new Message(owner + " faction already owns this chunk").fail().send(player, false);
            return 0;
        }
        chunks.add(chunkPos);


        chunks.forEach(chunk -> faction.addOutpost(chunk.x, chunk.z, dimension, new Claim.Outpost(player.getBlockX(), player.getBlockZ(), player.getBlockPos(), outpostName, dimension)));
            new Message("Chunk (%d, %d) claimed by %s", chunks.get(0).x, chunks.get(0).z, player.getName().getString())
                    .send(faction);

        return 1;
    }



    private int outpost(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = User.get(player.getName().getString()).getFaction();

        int requiredPower = FactionsMod.CONFIG.OUTPOST_COST;
        int maxPower = FactionsMod.CONFIG.MAX_POWER;


        ChunkPos chunkPos = player.getChunkPos();
        boolean isBordering = faction.getClaims().stream().anyMatch(claim -> (claim.x + 1 == chunkPos.x && claim.z == chunkPos.z) ||
                (claim.z + 1 == chunkPos.z && claim.x == chunkPos.x) ||
                (claim.x - 1 == chunkPos.x && claim.z == chunkPos.z) ||
                (claim.z - 1 == chunkPos.z && claim.x == chunkPos.x)) || faction.getClaims().isEmpty();
        if(isBordering) {
            new Message("The outpost is bordering with the base! Remember, this is an outpost, not a common claim! It must be far away from your town!").fail().send(player, false);
            return 0;
        }

        if(requiredPower > faction.getPower()){
            new Message("Not enough power; You have " + requiredPower + " power on your cash.").fail().send(player, false);
            return 0;
        }
        faction.adjustPower(-FactionsMod.CONFIG.OUTPOST_COST);
        return addForcedOutpost(context);
    }

    private int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = User.get(player.getName().getString()).getFaction();

        int requiredPower = FactionsMod.CONFIG.CLAIM_WEIGHT;
        ChunkPos chunkPos = player.getChunkPos();
        boolean isBordering = faction.getClaims().stream().anyMatch(claim -> (claim.x + 1 == chunkPos.x && claim.z == chunkPos.z) ||
                (claim.z + 1 == chunkPos.z && claim.x == chunkPos.x) ||
                (claim.x - 1 == chunkPos.x && claim.z == chunkPos.z) ||
                (claim.z - 1 == chunkPos.z && claim.x == chunkPos.x)) || faction.getClaims().isEmpty();
        if(!isBordering) {
            new Message("The chunk is not bordering with the base!").fail().send(player, false);
            return 0;
        }

        if (requiredPower > faction.getPower()) {
            new Message("Not enough faction power to claim chunk").fail().send(player, false);
            return 0;
        }
        faction.adjustPower(requiredPower);
        return addForced(context, 1);
    }

    private int addSize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = User.get(player.getName().getString()).getFaction();

        int requiredPower = (faction.getClaims().size() + (int)Math.pow(size * 2 - 1, 2)) * FactionsMod.CONFIG.CLAIM_WEIGHT;
        int maxPower = FactionsMod.CONFIG.MAX_POWER;

        if (maxPower < requiredPower) {
            new Message("Not enough faction power to claim chunks").fail().send(player, false);
            return 0;
        }

        return addForced(context, size);
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

        User user = User.get(player.getName().getString());
        Faction faction = user.getFaction();

        if (!user.bypass && existingClaim.getFaction().getID() != faction.getID()) {
            new Message("Cannot remove a claim owned by another faction").fail().send(player, false);
            return 0;
        }

        existingClaim.remove();
        new Message("Claim (%d, %d) removed by %s", existingClaim.x, existingClaim.z, player.getName().getString()).send(faction);
        return 1;
    }

    private int removeSize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();
        String dimension = world.getRegistryKey().getValue().toString();

        User user = User.get(player.getName().getString());
        Faction faction = user.getFaction();

        for (int x = -size + 1; x < size; x++) {
            for (int y = -size + 1; y < size; y++) {
                ChunkPos chunkPos = world.getChunk(player.getBlockPos().add(x * 16, 0, y * 16)).getPos();
                Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

                if (existingClaim != null && (user.bypass || existingClaim.getFaction().getID() == faction.getID())) existingClaim.remove();
            }
        }

        ChunkPos chunkPos = world.getChunk(player.getBlockPos().add((-size + 1) * 16, 0, (-size + 1) * 16)).getPos();
        new Message("Claims (%d, %d) to (%d, %d) removed by %s ", chunkPos.x, chunkPos.z,
                chunkPos.x + size - 1, chunkPos.z + size - 1, player.getName().getString())
            .send(faction);

        return 1;
    }

    private int removeAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = User.get(player.getName().getString()).getFaction();

        faction.removeAllClaims();
        new Message("All claims removed by %s", player.getName().getString()).send(faction);
        return 1;
    }

    private int auto(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getName().getString());
        user.autoclaim = !user.autoclaim;

        new Message("Successfully toggled autoclaim")
            .filler("·")
            .add(
                new Message(user.autoclaim ? "ON" : "OFF")
                    .format(user.autoclaim ? Formatting.GREEN : Formatting.RED)
            )
            .send(player, false);

        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("claim")
            .requires(Requires.isCommander())
            .then(
                CommandManager.literal("add")
                .requires(Requires.hasPerms("factions.claim.add", 0))
                .then(
                    CommandManager.argument("size", IntegerArgumentType.integer(1, 7))
                    .requires(Requires.hasPerms("factions.claim.add.size", 0))
                    .then(
                        CommandManager.literal("force")
                        .requires(Requires.hasPerms("factions.claim.add.force", 0))
                        .executes(context -> addForced(context, IntegerArgumentType.getInteger(context, "size")))
                    )
                    .executes(this::addSize)
                )
            .executes(this::add).then(CommandManager.literal("outpost").requires(Requires.isCommander().or(Requires.isOwner())).executes(this::outpost))
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
                    CommandManager.argument("size", IntegerArgumentType.integer(1, 7))
                    .requires(Requires.hasPerms("factions.claim.remove.size", 0))
                    .executes(this::removeSize)
                )
                .then(
                    CommandManager.literal("all")
                    .requires(Requires.hasPerms("factions.claim.remove.all", 0))
                    .executes(this::removeAll)
                )
                .executes(this::remove)
            )
            .then(
                CommandManager.literal("auto")
                .requires(Requires.hasPerms("factions.claim.auto", 0))
                .executes(this::auto)
            )
            .build();
    }
}