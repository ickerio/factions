package io.icker.factions.util;

import io.icker.factions.database.Faction;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import io.icker.factions.database.Ally;
import io.icker.factions.database.Member;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class FactionSuggestions {
    public static String[] general(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Member player = Member.get(context.getSource().getPlayer().getUuid());
        Faction faction = player.getFaction();

        return Faction.allBut(faction.name).stream().map(a -> Ally.checkIfAlly(a.name, faction.name) || Ally.checkIfAllyInvite(a.name, faction.name) ? null : a.name).toArray(String[]::new);
    }

    public static String[] allyInvites(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Member player = Member.get(context.getSource().getPlayer().getUuid());

        return Ally.getAllyInvites(player.getFaction().name).stream().map(a -> a.source).toArray(String[]::new);
    }

    public static String[] allies(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Member player = Member.get(context.getSource().getPlayer().getUuid());

        return Ally.getAll(player.getFaction().name).stream().map(a -> a.source == player.getFaction().name ? a.target : a.source).toArray(String[]::new);
    }
}