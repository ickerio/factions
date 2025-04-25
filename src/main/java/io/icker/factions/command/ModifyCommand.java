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
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.server.translations.api.Localization;

import java.util.Locale;

public class ModifyCommand implements Command {
    private int gui(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = Command.getUser(player).getFaction();

        new ModifyGui(player, faction, null);
        return 1;
    }

    private int name(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        try {
            execName(player, faction, name);
        } catch (Exception e) {
            new Message(e.getMessage()).fail()
                    .send(player, false);
            return 0;
        }

        new Message(Text.translatable("factions.gui.modify.change_name.result", name))
                .prependFaction(faction).send(player, false);

        return 1;
    }

    public static void execName(ServerPlayerEntity player, Faction faction, String name) throws Exception {
        if (FactionsMod.CONFIG.DISPLAY.NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
            throw new Exception(Localization.raw("factions.command.modify.name.fail.blacklisted_name", player));
        }

        if (FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH >= 0
                & FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH > name.length()) {
            throw new Exception(Localization.raw("factions.command.modify.name.fail.name_too_long", player));
        }

        if (Faction.getByName(name) != null) {
            throw new Exception(Localization.raw("factions.command.modify.name.fail.name_taken", player));
        }

        faction.setName(name);
    }

    private int description(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        String description = StringArgumentType.getString(context, "description");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setDescription(description);
        new Message(Text.translatable("factions.gui.modify.change_description.result", description))
                .prependFaction(faction).send(player, false);

        return 1;
    }

    private int motd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String motd = StringArgumentType.getString(context, "motd");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setMOTD(motd);
        new Message(Text.translatable("factions.gui.modify.change_motd.result", motd))
                .prependFaction(faction).send(player, false);

        return 1;
    }

    private int color(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Formatting color = ColorArgumentType.getColor(context, "color");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setColor(color);
        new Message(
                Text.translatable(
                        "factions.gui.modify.change_color.result",
                        Text.literal(color.name())
                                .setStyle(Style.EMPTY.withColor(color).withBold(true))))
                .prependFaction(faction).send(player, false);

        return 1;
    }

    private int open(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean open = BoolArgumentType.getBool(context, "open");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setOpen(open);
        new Message(Text.translatable("factions.command.modify.open.success"))
                .add(new Message(Text.translatable("factions.gui.modify.faction_type." + (open ? "public" : "invite")))
                        .format(open ? Formatting.GREEN : Formatting.RED))
                .prependFaction(faction).send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("modify").requires(Requires.isLeader())
                .requires(Requires.multiple(Requires.hasPerms("factions.modify.gui", 0),
                        Requires.isOwner()))
                .executes(this::gui)
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
