package io.icker.factions.command;

import java.util.Locale;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.server.translations.api.Localization;

public class CreateCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (FactionsMod.CONFIG.DISPLAY.NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
            new Message(Text.translatable("factions.command.create.fail.blacklisted_name")).fail()
                    .send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH >= 0
                & FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH < name.length()) {
            new Message(Text.translatable("factions.command.create.fail.name_too_long")).fail()
                    .send(player, false);
            return 0;
        }

        if (Faction.getByName(name) != null) {
            new Message(Text.translatable("factions.command.create.fail.name_taken")).fail()
                    .send(player, false);
            return 0;
        }

        Faction faction = new Faction(name, Localization.raw("factions.default_description", player),
                Localization.raw("factions.default_motd", player), Formatting.WHITE, false,
                FactionsMod.CONFIG.POWER.BASE + FactionsMod.CONFIG.POWER.MEMBER);
        Faction.add(faction);
        Command.getUser(player).joinFaction(faction.getID(), User.Rank.OWNER);

        source.getServer().getPlayerManager().sendCommandTree(player);
        new Message(Text.translatable("factions.command.create.success")).send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("create")
                .requires(Requires.multiple(Requires.isFactionless(),
                        Requires.hasPerms("factions.create", 0)))
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(this::run))
                .build();
    }
}
