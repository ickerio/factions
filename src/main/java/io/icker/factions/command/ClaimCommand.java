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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class ClaimCommand implements Command {
    private int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        List<Claim> claims = Command.getUser(player).getFaction().getClaims();
        int count = claims.size();

        new Message(
                        Component.translatable(
                                "factions.command.claim.list",
                                Component.literal(String.valueOf(count)).withStyle(ChatFormatting.YELLOW)))
                .send(source.getPlayerOrException(), false);

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
                            new Message(Component.translatable("factions.level." + level))
                                    .format(ChatFormatting.GRAY));
                    claimText.filler("»");
                    claimText.add(
                            array.stream()
                                    .map(claim -> String.format("(%d,%d)", claim.x, claim.z))
                                    .collect(Collectors.joining(", ")));
                });

        claimText.format(ChatFormatting.ITALIC).send(source.getPlayerOrException(), false);
        return 1;
    }

    private int addForced(CommandContext<CommandSourceStack> context, int size)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = (ServerLevel) player.level();

        Faction faction = Command.getUser(player).getFaction();
        String dimension = world.dimension().identifier().toString();
        ArrayList<ChunkPos> chunks = new ArrayList<ChunkPos>();

        for (int x = -size + 1; x < size; x++) {
            for (int y = -size + 1; y < size; y++) {
                ChunkPos chunkPos =
                        world.getChunk(player.blockPosition().offset(x * 16, 0, y * 16)).getPos();
                Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

                if (existingClaim != null) {
                    if (size == 1) {
                        boolean isActorOwner = existingClaim.getFaction().equals(faction);
                        new Message(
                                        Component.translatable(
                                                "factions.command.claim.add.fail.already_owned.single",
                                                Component.translatable(
                                                        "factions.command.claim.add.fail.already_owned.single."
                                                                + (isActorOwner
                                                                        ? "your"
                                                                        : "another"))))
                                .fail()
                                .send(player, false);
                        return 0;
                    } else if (!existingClaim.getFaction().equals(faction)) {
                        new Message(
                                        Component.translatable(
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
                            Component.translatable(
                                    "factions.command.claim.add.success.single",
                                    chunks.get(0).x,
                                    chunks.get(0).z,
                                    player.getName().getString()))
                    .send(faction);
        } else {
            new Message(
                            Component.translatable(
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

    private int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Faction faction = Command.getUser(player).getFaction();

        int requiredPower =
                (faction.getClaims().size() + 1) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower =
                faction.getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER
                        + FactionsMod.CONFIG.POWER.BASE
                        + faction.getAdminPower();

        if (maxPower < requiredPower) {
            new Message(Component.translatable("factions.command.claim.add.fail.lacks_power"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return addForced(context, 1);
    }

    private int addSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        ServerPlayer player = context.getSource().getPlayerOrException();
        Faction faction = Command.getUser(player).getFaction();

        int requiredPower =
                (faction.getClaims().size() + 1) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower =
                faction.getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER
                        + FactionsMod.CONFIG.POWER.BASE
                        + faction.getAdminPower();

        if (maxPower < requiredPower) {
            new Message(Component.translatable("factions.command.claim.add.fail.lacks_power.multiple"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return addForced(context, size);
    }

    private int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = (ServerLevel) player.level();

        ChunkPos chunkPos = world.getChunk(player.blockPosition()).getPos();
        String dimension = world.dimension().identifier().toString();

        Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        if (existingClaim == null) {
            new Message(Component.translatable("factions.command.claim.remove.fail.unclaimed"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        if (!user.bypass && existingClaim.getFaction().getID() != faction.getID()) {
            new Message(Component.translatable("factions.command.claim.remove.fail.another_owner"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        existingClaim.remove();
        new Message(
                        Component.translatable(
                                "factions.command.claim.remove.success.single",
                                existingClaim.x,
                                existingClaim.z,
                                player.getName().getString()))
                .send(faction);
        return 1;
    }

    private int removeSize(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = (ServerLevel) player.level();
        String dimension = world.dimension().identifier().toString();

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        for (int x = -size + 1; x < size; x++) {
            for (int y = -size + 1; y < size; y++) {
                ChunkPos chunkPos =
                        world.getChunk(player.blockPosition().offset(x * 16, 0, y * 16)).getPos();
                Claim existingClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);

                if (existingClaim != null
                        && (user.bypass || existingClaim.getFaction().equals(faction)))
                    existingClaim.remove();
            }
        }

        ChunkPos chunkPos =
                world.getChunk(player.blockPosition().offset((-size + 1) * 16, 0, (-size + 1) * 16))
                        .getPos();
        new Message(
                        Component.translatable(
                                "factions.command.claim.remove.success.multiple",
                                chunkPos.x,
                                chunkPos.z,
                                chunkPos.x + size - 1,
                                chunkPos.z + size - 1,
                                player.getName().getString()))
                .send(faction);

        return 1;
    }

    private int removeAll(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(player).getFaction();

        faction.removeAllClaims();
        new Message(
                        Component.translatable(
                                "factions.command.claim.remove.success.all",
                                player.getName().getString()))
                .send(faction);
        return 1;
    }

    private int auto(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        User user = Command.getUser(player);
        user.autoclaim = !user.autoclaim;

        new Message(Component.translatable("factions.command.claim.auto.toggled"))
                .filler("·")
                .add(
                        new Message(Component.translatable("options." + (user.autoclaim ? "on" : "off")))
                                .format(user.autoclaim ? ChatFormatting.GREEN : ChatFormatting.RED))
                .send(player, false);

        return 1;
    }

    @SuppressWarnings("")
    private int setAccessLevel(CommandContext<CommandSourceStack> context, boolean increase)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = (ServerLevel) player.level();

        ChunkPos chunkPos = world.getChunk(player.blockPosition()).getPos();
        String dimension = world.dimension().identifier().toString();

        Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);

        if (claim == null) {
            new Message(Component.translatable("factions.command.claim.set_access_level.fail.unclaimed"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        if (!user.bypass && claim.getFaction().getID() != faction.getID()) {
            new Message(
                            Component.translatable(
                                    "factions.command.claim.set_access_level.fail.another_owner"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (increase) {
            if (claim.accessLevel.ordinal() <= user.rank.ordinal()) {
                new Message(
                                Component.translatable(
                                        "factions.command.claim.set_access_level.fail.max_level_for_rank"))
                        .fail()
                        .send(player, false);
                return 0;
            }

            switch (claim.accessLevel) {
                case OWNER -> {
                    new Message(
                                    Component.translatable(
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
                                Component.translatable(
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
                                    Component.translatable(
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
                        Component.translatable(
                                "factions.command.claim.set_access_level.success",
                                claim.x,
                                claim.z,
                                claim.accessLevel.toString(),
                                player.getName().getString()))
                .send(faction);
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("claim")
                .requires(Requires.isCommander())
                .then(
                        Commands.literal("add")
                                .requires(Requires.hasPerms("factions.claim.add", 0))
                                .then(
                                        Commands.argument(
                                                        "size", IntegerArgumentType.integer(1, 7))
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.add.size", 0))
                                                .then(
                                                        Commands.literal("force")
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
                        Commands.literal("list")
                                .requires(Requires.hasPerms("factions.claim.list", 0))
                                .executes(this::list))
                .then(
                        Commands.literal("remove")
                                .requires(
                                        Requires.hasPerms("factions.claim.remove", 0)
                                                .and(Requires.isLeader()))
                                .then(
                                        Commands.argument(
                                                        "size", IntegerArgumentType.integer(1, 7))
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.remove.size", 0))
                                                .executes(this::removeSize))
                                .then(
                                        Commands.literal("all")
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.remove.all", 0))
                                                .executes(this::removeAll))
                                .executes(this::remove))
                .then(
                        Commands.literal("auto")
                                .requires(Requires.hasPerms("factions.claim.auto", 0))
                                .executes(this::auto))
                .then(
                        Commands.literal("access")
                                .requires(Requires.hasPerms("factions.claim.access", 0))
                                .then(
                                        Commands.literal("increase")
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.claim.access.increase",
                                                                0))
                                                .executes(
                                                        (context) -> setAccessLevel(context, true)))
                                .then(
                                        Commands.literal("decrease")
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
