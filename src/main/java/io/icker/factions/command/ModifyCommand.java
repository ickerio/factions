package io.icker.factions.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.ui.ModifyGui;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import xyz.nucleoid.server.translations.api.Localization;

import java.util.Locale;

public class ModifyCommand implements Command {
    private int gui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Faction faction = Command.getUser(player).getFaction();

        new ModifyGui(player, faction, null);
        return 1;
    }

    private int name(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(player).getFaction();

        try {
            execName(player, faction, name);
        } catch (Exception e) {
            new Message(e.getMessage()).fail().send(player, false);
            return 0;
        }

        new Message(Component.translatable("factions.gui.modify.change_name.result", name))
                .prependFaction(faction)
                .send(player, false);

        return 1;
    }

    public static void execName(ServerPlayer player, Faction faction, String name)
            throws Exception {
        if (FactionsMod.CONFIG.DISPLAY.NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
            throw new Exception(
                    Localization.raw("factions.command.modify.name.fail.blacklisted_name", player));
        }

        if (FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH > 0
                && FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH < name.length()) {
            throw new Exception(
                    Localization.raw("factions.command.modify.name.fail.name_too_long", player));
        }

        if (Faction.getByName(name) != null) {
            throw new Exception(
                    Localization.raw("factions.command.modify.name.fail.name_taken", player));
        }

        faction.setName(name);
    }

    private int description(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String description = StringArgumentType.getString(context, "description");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(player).getFaction();

        faction.setDescription(description);
        new Message(
                        Component.translatable(
                                "factions.gui.modify.change_description.result", description))
                .prependFaction(faction)
                .send(player, false);

        return 1;
    }

    private int motd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String motd = StringArgumentType.getString(context, "motd");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(player).getFaction();

        faction.setMOTD(motd);
        new Message(Component.translatable("factions.gui.modify.change_motd.result", motd))
                .prependFaction(faction)
                .send(player, false);

        return 1;
    }

    private int color(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ChatFormatting color = ColorArgument.getColor(context, "color");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(player).getFaction();

        faction.setColor(color);

        if (color.equals(ChatFormatting.RESET)) {
            new Message(Component.translatable("factions.gui.modify.change_color.result.reset"))
                    .prependFaction(faction)
                    .send(player, false);
        } else {
            new Message(
                            Component.translatable(
                                    "factions.gui.modify.change_color.result.color",
                                    Component.translatable(
                                                    "factions.gui.modify.change_color.color."
                                                            + color.name().toLowerCase())
                                            .setStyle(Style.EMPTY.withColor(color).withBold(true))))
                    .prependFaction(faction)
                    .send(player, false);
        }

        return 1;
    }

    private int open(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean open = BoolArgumentType.getBool(context, "open");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(player).getFaction();

        faction.setOpen(open);
        new Message(Component.translatable("factions.command.modify.open.success"))
                .add(
                        new Message(
                                        Component.translatable(
                                                "factions.gui.modify.faction_type."
                                                        + (open ? "public" : "invite")))
                                .format(open ? ChatFormatting.GREEN : ChatFormatting.RED))
                .prependFaction(faction)
                .send(player, false);

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("modify")
                .requires(Requires.isLeader())
                .requires(
                        Requires.multiple(
                                Requires.hasPerms("factions.modify.gui", 0), Requires.isOwner()))
                .executes(this::gui)
                .then(
                        Commands.literal("name")
                                .requires(
                                        Requires.multiple(
                                                Requires.hasPerms("factions.modify.name", 0),
                                                Requires.isOwner()))
                                .then(
                                        Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(this::name)))
                .then(
                        Commands.literal("description")
                                .requires(Requires.hasPerms("factions.modify.description", 0))
                                .then(
                                        Commands.argument(
                                                        "description",
                                                        StringArgumentType.greedyString())
                                                .executes(this::description)))
                .then(
                        Commands.literal("motd")
                                .requires(Requires.hasPerms("factions.modify.motd", 0))
                                .then(
                                        Commands.argument("motd", StringArgumentType.greedyString())
                                                .executes(this::motd)))
                .then(
                        Commands.literal("color")
                                .requires(Requires.hasPerms("factions.modify.color", 0))
                                .then(
                                        Commands.argument("color", ColorArgument.color())
                                                .executes(this::color)))
                .then(
                        Commands.literal("open")
                                .requires(Requires.hasPerms("factions.modify.open", 0))
                                .then(
                                        Commands.argument("open", BoolArgumentType.bool())
                                                .executes(this::open)))
                .build();
    }
}
