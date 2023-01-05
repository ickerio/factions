package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.Message;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import io.icker.factions.util.Translator;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.Locale;

public class CreateCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (FactionsMod.CONFIG.DISPLAY.NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
            new Message().append(new TranslatableText("translate:create.error.blacklist").fail()).send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH >= 0 & FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH < name.length()) {
            new Message().append(new TranslatableText("translate:create.error.length").fail()).send(player, false);
            return 0;
        }

        if (Faction.getByName(name) != null) {
            new Message().append(new TranslatableText("translate:create.error.exists").fail()).send(player, false);
            return 0;
        }

        Faction faction = new Faction(name, Translator.get("translate:desc", User.get(player.getUuid()).language), Translator.get("translate:motd", User.get(player.getUuid()).language), Formatting.WHITE, false, FactionsMod.CONFIG.POWER.BASE + FactionsMod.CONFIG.POWER.MEMBER);
        Faction.add(faction);
        Command.getUser(player).joinFaction(faction.getID(), User.Rank.OWNER);

        source.getServer().getPlayerManager().sendCommandTree(player);
        new Message().append(new TranslatableText("translate:create")).send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("create")
            .requires(Requires.multiple(Requires.isFactionless(), Requires.hasPerms("factions.create", 0)))
            .then(
                CommandManager.argument("name", StringArgumentType.greedyString()).executes(this::run)
            )
            .build();
    }
}