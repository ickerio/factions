package io.icker.factions.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Ally;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Invite;
import io.icker.factions.database.Member;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.Arrays;

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

    public static String[] avaliableFactions(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ArrayList<String> invites = new ArrayList<>(Arrays.asList(Invite.get(context.getSource().getPlayer().getUuid()).stream().map(a -> a.getFaction().name).toArray(String[]::new)));

        return Faction.all().stream().map(a -> a.open ? a.name : invites.contains(a.name) ? a.name : null).toArray(String[]::new);
    }

    public static String[] all(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Faction.all().stream().map(a -> a.name).toArray(String[]::new);
    }
}