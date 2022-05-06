package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Ally;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public class AllyCommand {
    public static int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Faction target = Faction.get(StringArgumentType.getString(context, "faction"));

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction sourceFaction = Member.get(player.getUuid()).getFaction();

        if (Ally.checkIfAlly(sourceFaction.name, target.name) || Ally.checkIfAllyInvite(sourceFaction.name, target.name)) {
            new Message(target.name + " is already allied or invited").format(Formatting.RED).send(player, false);
        } else if (sourceFaction.name == target.name) {
            new Message("You can't ally yourself").format(Formatting.RED).send(player, false);
        } else {
            Ally.add(sourceFaction.name, target.name);

            new Message(target.name + " is now invited to be an ally")
                    .send(player, false);
            new Message(
                    "You have been invited to be an ally with " + sourceFaction.name).format(Formatting.YELLOW)
                    .hover("Click to accept the invitation").click("/factions ally accept " + sourceFaction.name)
                    .send(target);
        }

        return 1;
    }

    public static int accept(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Faction sourceFaction = Faction.get(StringArgumentType.getString(context, "faction"));

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction targetFaction = Member.get(player.getUuid()).getFaction();

        if (Ally.checkIfAlly(sourceFaction.name, targetFaction.name) || !Ally.checkIfAllyInvite(sourceFaction.name, targetFaction.name)) {
            new Message(targetFaction.name + " is already allied or has not invited you").format(Formatting.RED).send(player, false);
        } else if (sourceFaction.name == targetFaction.name) {
            new Message("You can't ally yourself").format(Formatting.RED).send(player, false);
        } else {
            Ally.accept(sourceFaction.name, targetFaction.name);

            new Message("You are now allies with "
                    + sourceFaction.name).format(Formatting.YELLOW)
                    .send(targetFaction);
            new Message(
                    "You are now allies with " + targetFaction.name).format(Formatting.YELLOW)
                    .send(sourceFaction);
        }

        return 1;
    }

    public static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Member.get(player.getUuid()).getFaction();

        ArrayList<Ally> invites = Ally.getAllyInvites(faction.name);
        ArrayList<Ally> sentInvites = Ally.getSentInvites(faction.name);

        if (invites.size() + sentInvites.size() == 0) {
            new Message("You have no invites").send(player, false);
        } else {
            for (Ally ally : invites) {
                new Message(
                        ally.source + " has invited you")
                        .hover("Click to accept them as an ally").click("/factions ally accept " + ally.source)
                        .send(player, false);
            }
            for (Ally ally : sentInvites) {
                new Message(
                        "You have invited " + ally.target)
                        .hover("Click to remove the invite").click("/factions ally remove " + ally.target)
                        .send(player, false);
            }
        }

        return 1;
    }

    public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Faction target = Faction.get(StringArgumentType.getString(context, "faction"));

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction sourceFaction = Member.get(player.getUuid()).getFaction();

        if (!Ally.checkIfAlly(sourceFaction.name, target.name) && !Ally.checkIfAllyInvite(sourceFaction.name, target.name)) {
            new Message(target.name + " is not allied").format(Formatting.RED).send(player, false);
        } else {
            Ally.remove(sourceFaction.name, target.name);

            new Message(target.name + " is no longer allied")
                    .send(sourceFaction);
            new Message(
                    "You are no longer allies with " + sourceFaction.name).format(Formatting.YELLOW)
                    .send(target);
        }

        return 1;
    }
}