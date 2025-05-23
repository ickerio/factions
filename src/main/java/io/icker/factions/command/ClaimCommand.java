package io.icker.factions.command;

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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ClaimCommand implements Command {
    private int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        List<Claim> claims = Command.getUser(player).getFaction().getClaims();
        int count = claims.size();

        new Message(
                        Text.translatable(
                                "factions.command.claim.list",
                                Text.literal(String.valueOf(count)).formatted(Formatting.YELLOW)))
                .send(source.getPlayerOrThrow(), false);

        if (count == 0) return 1;

        HashMap<String, ArrayList<Claim>> claimsMap = new HashMap<String, ArrayList<Claim>>();

        claims.forEach(
                claim -> {
                    claimsMap.putIfAbsent(claim.level, new ArrayList<Claim>());
                    claimsMap.get(claim.level).add(claim);
                });

        Message claimText = new Message();
        claimsMap.forEach(
                (level, array) -> {
                    claimText.add("\n");
                    claimText.add(
                            new Message(Text.translatable("factions.level." + level))
                                    .format(Formatting.GRAY));
                    claimText.filler("»");
                    claimText.add(
                            array.stream()
                                    .map(claim -> String.format("(%d,%d)", claim.x, claim.z))
                                    .collect(Collectors.joining(", ")));
                });

        claimText.format(Formatting.ITALIC).send(source.getPlayerOrThrow(), false);
        return 1;
    }

    private int addForced(CommandContext<ServerCommandSource> context, int size)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = (ServerWorld) player.getWorld();

        Faction faction = Command.getUser(player).getFaction();
        String dimension = world.getRegistryKey().getValue().toString();
        ArrayList<ChunkPos> chunks = new ArrayList<ChunkPos>();

        for (int x = -size + 1; x < size; x++) {
            for (int y = -size + 1; y < size; y++) {
                ChunkPos chunkPos =
                        world.getChunk(player.getBlockPos().add(x * 16, 0, y * 16)).getPos();
                Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

                if (existingClaim != null) {
                    if (size == 1) {
                        boolean isActorOwner = existingClaim.getFaction().equals(faction);
                        new Message(
                                        Text.translatable(
                                                "factions.command.claim.add.fail.already_owned.single",
                                                Text.translatable(
                                                        "factions.command.claim.add.fail.already_owned.single."
                                                                + (isActorOwner
                                                                        ? "your"
                                                                        : "another"))))
                                .fail()
                                .send(player, false);
                        return 0;
                    } else if (!existingClaim.getFaction().equals(faction)) {
                        new Message(
                                        Text.translatable(
                                                "factions.command.claim.add.fail.already_owned.multiple"))
                                .fail()
                                .send(player, false);
                        return 0;
                    }
                }

                chunks.add(chunkPos);
            }
        }

        chunks.forEach(chunk -> faction.addClaim(chunk.x, chunk.z, dimension));
        if (size == 1) {
            new Message(
                            Text.translatable(
                                    "factions.command.claim.add.success.single",
                                    chunks.get(0).x,
                                    chunks.get(0).z,
                                    player.getName().getString()))
                    .send(faction);
        } else {
            new Message(
                            Text.translatable(
                                    "factions.command.claim.add.success.multiple",
                                    chunks.get(0).x,
                                    chunks.get(0).z,
                                    chunks.get(0).x + size - 1,
                                    chunks.get(0).z + size - 1,
                                    player.getName().getString()))
                    .send(faction);
        }

        return 1;
    }

    private int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Faction faction = Command.getUser(player).getFaction();

        int requiredPower =
                (faction.getClaims().size() + 1) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower =
                faction.getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER
                        + FactionsMod.CONFIG.POWER.BASE
                        + faction.getAdminPower();

        if (maxPower < requiredPower) {
            new Message(Text.translatable("factions.command.claim.add.fail.lacks_power"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return addForced(context, 1);
    }

    private int addSize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Faction faction = Command.getUser(player).getFaction();

        int requiredPower =
                (faction.getClaims().size() + 1) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower =
                faction.getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER
                        + FactionsMod.CONFIG.POWER.BASE
                        + faction.getAdminPower();

        if (maxPower < requiredPower) {
            new Message(Text.translatable("factions.command.claim.add.fail.lacks_power.multiple"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return addForced(context, size);
    }

    private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = (ServerWorld) player.getWorld();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        String dimension = world.getRegistryKey().getValue().toString();

        Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        if (existingClaim == null) {
            new Message(Text.translatable("factions.command.claim.remove.fail.unclaimed"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        if (!user.bypass && existingClaim.getFaction().getID() != faction.getID()) {
            new Message(Text.translatable("factions.command.claim.remove.fail.another_owner"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        existingClaim.remove();
        new Message(
                        Text.translatable(
                                "factions.command.claim.remove.success.single",
                                existingClaim.x,
                                existingClaim.z,
                                player.getName().getString()))
                .send(faction);
        return 1;
    }

    private int removeSize(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = (ServerWorld) player.getWorld();
        String dimension = world.getRegistryKey().getValue().toString();

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        for (int x = -size + 1; x < size; x++) {
            for (int y = -size + 1; y < size; y++) {
                ChunkPos chunkPos =
                        world.getChunk(player.getBlockPos().add(x * 16, 0, y * 16)).getPos();
                Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

                if (existingClaim != null
                        && (user.bypass || existingClaim.getFaction().equals(faction)))
                    existingClaim.remove();
            }
        }

        ChunkPos chunkPos =
                world.getChunk(player.getBlockPos().add((-size + 1) * 16, 0, (-size + 1) * 16))
                        .getPos();
        new Message(
                        Text.translatable(
                                "factions.command.claim.remove.success.multiple",
                                chunkPos.x,
                                chunkPos.z,
                                chunkPos.x + size - 1,
                                chunkPos.z + size - 1,
                                player.getName().getString()))
                .send(faction);

        return 1;
    }

    private int removeAll(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        Faction faction = Command.getUser(player).getFaction();

        faction.removeAllClaims();
        new Message(
                        Text.translatable(
                                "factions.command.claim.remove.success.all",
                                player.getName().getString()))
                .send(faction);
        return 1;
    }

    private int auto(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        User user = Command.getUser(player);
        user.autoclaim = !user.autoclaim;

        new Message(Text.translatable("factions.command.claim.auto.toggled"))
                .filler("·")
                .add(
                        new Message(Text.translatable("options." + (user.autoclaim ? "on" : "off")))
                                .format(user.autoclaim ? Formatting.GREEN : Formatting.RED))
                .send(player, false);

        return 1;
    }

    @SuppressWarnings("")
    private int setAccessLevel(CommandContext<ServerCommandSource> context, boolean increase)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = (ServerWorld) player.getWorld();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        String dimension = world.getRegistryKey().getValue().toString();

        Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        if (claim == null) {
            new Message(Text.translatable("factions.command.claim.set_access_level.fail.unclaimed"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        if (!user.bypass && claim.getFaction().getID() != faction.getID()) {
            new Message(
                            Text.translatable(
                                    "factions.command.claim.set_access_level.fail.another_owner"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (increase) {
            if (claim.accessLevel.ordinal() <= user.rank.ordinal()) {
                new Message(
                                Text.translatable(
                                        "factions.command.claim.set_access_level.fail.max_level_for_rank"))
                        .fail()
                        .send(player, false);
                return 0;
            }

            switch (claim.accessLevel) {
                case OWNER -> {
                    new Message(
                                    Text.translatable(
                                            "factions.command.claim.set_access_level.fail.max_level"))
                            .fail()
                            .send(player, false);
                    return 0;
                }
                case LEADER -> claim.accessLevel = User.Rank.OWNER;
                case COMMANDER -> claim.accessLevel = User.Rank.LEADER;
                case MEMBER -> claim.accessLevel = User.Rank.COMMANDER;
                case GUEST -> {
                    return 0; // Should be unreachable unless state is invalid
                }
            }
        } else {
            if (claim.accessLevel.ordinal() <= user.rank.ordinal()) {
                new Message(
                                Text.translatable(
                                        "factions.command.claim.set_access_level.fail.rank_too_low"))
                        .fail()
                        .send(player, false);
                return 0;
            }

            switch (claim.accessLevel) {
                case OWNER -> claim.accessLevel = User.Rank.LEADER;
                case LEADER -> claim.accessLevel = User.Rank.COMMANDER;
                case COMMANDER -> claim.accessLevel = User.Rank.MEMBER;
                case MEMBER -> {
                    new Message(
                                    Text.translatable(
                                            "factions.command.claim.set_access_level.fail.min_level"))
                            .fail()
                            .send(player, false);
                    return 0;
                }
                case GUEST -> {
                    return 0; // Should be unreachable unless state is invalid
                }
            }
        }

        new Message(
                        Text.translatable(
                                "factions.command.claim.set_access_level.success",
                                claim.x,
                                claim.z,
                                claim.accessLevel.toString(),
                                player.getName().getString()))
                .send(faction);
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("claim")
                .requires(Requires.isCommander())
                .then(
                        CommandManager.literal("add")
                                .requires(Requires.hasPerms("factions.claim.add", 0))
                                .then(
                                        CommandManager.argument(
                                                        "size", IntegerArgumentType.integer(1, 7))
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.add.size", 0))
                                                .then(
                                                        CommandManager.literal("force")
                                                                .requires(
                                                                        Requires.hasPerms(
                                                                                        "factions.claim.add.force",
                                                                                        0)
                                                                                .and(
                                                                                        Requires
                                                                                                .isLeader()))
                                                                .executes(
                                                                        context ->
                                                                                addForced(
                                                                                        context,
                                                                                        IntegerArgumentType
                                                                                                .getInteger(
                                                                                                        context,
                                                                                                        "size"))))
                                                .executes(this::addSize))
                                .executes(this::add))
                .then(
                        CommandManager.literal("list")
                                .requires(Requires.hasPerms("factions.claim.list", 0))
                                .executes(this::list))
                .then(
                        CommandManager.literal("remove")
                                .requires(
                                        Requires.hasPerms("factions.claim.remove", 0)
                                                .and(Requires.isLeader()))
                                .then(
                                        CommandManager.argument(
                                                        "size", IntegerArgumentType.integer(1, 7))
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.remove.size", 0))
                                                .executes(this::removeSize))
                                .then(
                                        CommandManager.literal("all")
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.remove.all", 0))
                                                .executes(this::removeAll))
                                .executes(this::remove))
                .then(
                        CommandManager.literal("auto")
                                .requires(Requires.hasPerms("factions.claim.auto", 0))
                                .executes(this::auto))
                .then(
                        CommandManager.literal("access")
                                .requires(Requires.hasPerms("factions.claim.access", 0))
                                .then(
                                        CommandManager.literal("increase")
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.access.increase",
                                                                0))
                                                .executes(
                                                        (context) -> setAccessLevel(context, true)))
                                .then(
                                        CommandManager.literal("decrease")
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.access.decrease",
                                                                0))
                                                .executes(
                                                        (context) ->
                                                                setAccessLevel(context, false))))
                .build();
    }
}
