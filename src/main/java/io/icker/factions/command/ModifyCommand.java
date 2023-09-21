package io.icker.factions.command;

import java.util.Locale;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class ModifyCommand implements Command {
    private int name(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (FactionsMod.CONFIG.DISPLAY.NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
            new Message("Cannot rename a faction to that name as it is on the blacklist").fail()
                    .send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH >= 0
                & FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH > name.length()) {
            new Message("Cannot rename a faction to this that as it is too long").fail()
                    .send(player, false);
            return 0;
        }

        if (Faction.getByName(name) != null) {
            new Message("A faction with that name already exists").fail().send(player, false);
            return 0;
        }

        Faction faction = Command.getUser(player).getFaction();

        faction.setName(name);
        new Message("Successfully renamed faction to '" + name + "'").prependFaction(faction)
                .send(player, false);

        return 1;
    }

    private int description(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        String description = StringArgumentType.getString(context, "description");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setDescription(description);
        new Message("Successfully updated faction description to '" + description + "'")
                .prependFaction(faction).send(player, false);

        return 1;
    }

    private int motd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String motd = StringArgumentType.getString(context, "motd");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setMOTD(motd);
        new Message("Successfully updated faction MOTD to '" + motd + "'").prependFaction(faction)
                .send(player, false);

        return 1;
    }

    private int color(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Formatting color = ColorArgumentType.getColor(context, "color");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setColor(color);
        new Message(
                "Successfully updated faction color to " + Formatting.BOLD + color + color.name())
                        .prependFaction(faction).send(player, false);

        return 1;
    }

    private int open(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean open = BoolArgumentType.getBool(context, "open");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setOpen(open);
        new Message("Successfully updated faction to ")
                .add(new Message(open ? "Open" : "Closed")
                        .format(open ? Formatting.GREEN : Formatting.RED))
                .prependFaction(faction).send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("modify").requires(Requires.isLeader())
                .then(CommandManager.literal("name")
                        .requires(Requires.multiple(Requires.hasPerms("factions.modify.name", 0),
                                Requires.isOwner()))
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(this::name)))
                .then(CommandManager.literal("description")
                        .requires(Requires.hasPerms("factions.modify.description", 0))
                        .then(CommandManager
                                .argument("description", StringArgumentType.greedyString())
                                .executes(this::description)))
                .then(CommandManager.literal("motd")
                        .requires(Requires.hasPerms("factions.modify.motd", 0))
                        .then(CommandManager.argument("motd", StringArgumentType.greedyString())
                                .executes(this::motd)))
                .then(CommandManager.literal("color")
                        .requires(Requires.hasPerms("factions.modify.color", 0))
                        .then(CommandManager.argument("color", ColorArgumentType.color())
                                .executes(this::color)))
                .then(CommandManager.literal("open")
                        .requires(Requires.hasPerms("factions.modify.open", 0)).then(CommandManager
                                .argument("open", BoolArgumentType.bool()).executes(this::open)))
                .build();
    }
}
