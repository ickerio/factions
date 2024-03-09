package io.icker.factions.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class ModifyCommand implements Command {
    private int name(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (Faction.getByName(name) != null) {
            new Message("A faction with that name already exists").fail().send(player, false);
            return 0;
        }

        Faction faction = User.get(player.getName().getString()).getFaction();

        faction.setName(name);
        new Message("Successfully renamed faction to '" + name + "'")
            .prependFaction(faction)
            .send(player, false);

        return 1;
    }

    private int description(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String description = StringArgumentType.getString(context, "description");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = User.get(player.getName().getString()).getFaction();

        faction.setDescription(description);
        new Message("Successfully updated faction description to '" + description + "'")
            .prependFaction(faction)
            .send(player, false);

        return 1;
    }

    private int motd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String motd = StringArgumentType.getString(context, "motd");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = User.get(player.getName().getString()).getFaction();

        faction.setMOTD(motd);
        new Message("Successfully updated faction MOTD to '" + motd + "'")
            .prependFaction(faction)
            .send(player, false);

        return 1;
    }

    private int color(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Formatting color = ColorArgumentType.getColor(context, "color");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = User.get(player.getName().getString()).getFaction();

        faction.setColor(color);
        new Message("Successfully updated faction color to " + Formatting.BOLD + color + color.name())
            .prependFaction(faction)
            .send(player, false);

        return 1;
    }

    public int admin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean admin = BoolArgumentType.getBool(context, "admin");
        String name = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        Faction faction = Faction.getByName(name);

        faction.setAdmin(admin);
        new Message("Successfully updated adminpower to faction: " + Formatting.UNDERLINE + name).send(player, false);
        return 0;
    }

    private int open(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean open = BoolArgumentType.getBool(context, "open");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = User.get(player.getName().getString()).getFaction();

        faction.setOpen(open);
        new Message("Successfully updated faction to ")
            .add(
                new Message(open ? "Open" : "Closed")
                    .format(open ? Formatting.GREEN : Formatting.RED)
            )
            .prependFaction(faction)
            .send(player, false);
            
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("modify")
            .requires(Requires.isLeader())
            .then(
                CommandManager
                .literal("name")
                .requires(Requires.multiple(Requires.hasPerms("factions.modify.name", 0), Requires.isOwner()))
                .then(
                    CommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(this::name)
                )
            )
            .then(
                CommandManager
                .literal("description")
                .requires(Requires.hasPerms("factions.modify.description", 0))
                .then(
                    CommandManager.argument("description", StringArgumentType.greedyString())
                    .executes(this::description)
                )
            )
            .then(
                CommandManager
                .literal("motd")
                .requires(Requires.hasPerms("factions.modify.motd", 0))
                .then(
                    CommandManager.argument("motd", StringArgumentType.greedyString())
                    .executes(this::motd)
                )
            )
            .then(
                CommandManager
                .literal("color")
                .requires(Requires.hasPerms("factions.modify.color", 0))
                .then(
                    CommandManager.argument("color", ColorArgumentType.color())
                    .executes(this::color)
                )
            )
            .then(
                CommandManager
                .literal("open")
                .requires(Requires.hasPerms("factions.modify.open", 0))
                .then(
                    CommandManager.argument("open", BoolArgumentType.bool())
                    .executes(this::open)
                )
            )
                .then(
                        CommandManager
                                .literal("admin").requires(Requires.hasPerms("factions.admin.bypass", 4))
                                .then(
                                        CommandManager.argument("name", StringArgumentType.string()).then(
                                                CommandManager.argument("admin", BoolArgumentType.bool())
                                                        .executes(this::admin)
                                                )
                                )
                ).then(
                        CommandManager.literal("chunk").requires(Requires.isOwner().or(Requires.isLeader().or(Requires.isCommander()))).then(
                                CommandManager.literal("create-compat").then(
                                        CommandManager.argument("create-compat", BoolArgumentType.bool())
                                                .executes(this::createCompat)
                                )
                        )
                )
                .build();
    }

    private int createCompat(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean create = BoolArgumentType.getBool(context, "create-compat");

        if(context.getSource().getPlayer() == null) {
            new Message("You must be the player!").fail();
            return 1;
        }

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        World world = player.getWorld();

        String dimension = world.getRegistryKey().getValue().toString();
        Claim claim = Claim.get(player.getBlockX()>>4, player.getBlockZ()>>4, dimension);
        if(claim == null) {
            new Message("There are no chunks claimed!").fail().send(player, false);
            return 1;
        }
        Faction playerFaction = User.get(player.getName().getString()).getFaction();
        if(claim.getFaction().getID() != playerFaction.getID()) {
            new Message("You don't owe this lands!").fail().send(player, false);
            return 1;
        }
        claim.create = create;
        return 0;
    }
}
