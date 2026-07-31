package io.icker.factions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.GuestGrant;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class GuestCommand implements Command {
    private UUID resolvePlayer(CommandContext<CommandSourceStack> context, String name)
            throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(name);
        return target == null ? null : target.getUUID();
    }

    private boolean hasFactionMember(Faction faction, UUID playerID) {
        User user = User.get(playerID);
        return user.isInFaction()
                && user.getFaction() != null
                && user.getFaction().getID().equals(faction.getID());
    }

    private int grant(CommandContext<CommandSourceStack> context, boolean breakBlocks)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        User actorUser = Command.getUser(player);
        Faction faction = actorUser.getFaction();
        String name = StringArgumentType.getString(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        UUID targetID = resolvePlayer(context, name);

        if (targetID == null) {
            new Message(
                            Component.translatable(
                                    "factions.command.guest.grant.fail.player_not_found"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (hasFactionMember(faction, targetID)) {
            new Message(
                            Component.translatable(
                                    "factions.command.guest.grant.fail.already_member"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        int maximum =
                breakBlocks
                        ? FactionsMod.CONFIG.GUEST_GRANT.MAX_BREAK
                        : FactionsMod.CONFIG.GUEST_GRANT.MAX_PLACE;
        GuestGrant grant = GuestGrant.get(faction.getID(), targetID);
        int remaining = grant == null ? 0 : (breakBlocks ? grant.breakRemaining : grant.placeRemaining);
        if (remaining > maximum || amount > maximum - remaining) {
            new Message(
                            Component.translatable(
                                    "factions.command.guest.grant.fail.max_exceeded"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (grant == null) {
            grant =
                    new GuestGrant(
                            faction.getID(),
                            targetID,
                            breakBlocks ? amount : 0,
                            breakBlocks ? 0 : amount);
        } else if (breakBlocks) {
            grant.breakRemaining += amount;
        } else {
            grant.placeRemaining += amount;
        }

        GuestGrant.add(grant);
        GuestGrant.save();
        new Message(
                        Component.translatable(
                                breakBlocks
                                        ? "factions.command.guest.grant.success.break"
                                        : "factions.command.guest.grant.success.place"))
                .send(player, false);
        return 1;
    }

    private int grantBreak(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        return grant(context, true);
    }

    private int grantPlace(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        return grant(context, false);
    }

    private int revoke(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Faction faction = Command.getUser(player).getFaction();
        UUID targetID = resolvePlayer(context, StringArgumentType.getString(context, "player"));

        if (targetID == null) {
            new Message(
                            Component.translatable(
                                    "factions.command.guest.grant.fail.player_not_found"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        GuestGrant grant = GuestGrant.get(faction.getID(), targetID);
        if (grant == null) {
            new Message(
                            Component.translatable("factions.command.guest.revoke.fail.no_grant"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        grant.remove();
        GuestGrant.save();
        new Message(Component.translatable("factions.command.guest.revoke.success"))
                .send(player, false);
        return 1;
    }

    private int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Faction faction = Command.getUser(player).getFaction();
        new Message(Component.translatable("factions.command.guest.list.header"))
                .send(player, false);

        for (GuestGrant grant : GuestGrant.getByFaction(faction.getID())) {
            ServerPlayer grantedPlayer =
                    context.getSource().getServer().getPlayerList().getPlayer(grant.playerID);
            String name =
                    grantedPlayer != null ? grantedPlayer.getName().getString() : grant.playerID.toString();
            new Message(
                            Component.translatable(
                                    "factions.command.guest.list.entry",
                                    name,
                                    grant.breakRemaining,
                                    grant.placeRemaining))
                    .send(player, false);
        }
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("guest")
                .requires(Requires.isLeader())
                .then(
                        Commands.literal("grant")
                                .then(
                                                        Commands.argument(
                                                                        "player", StringArgumentType.word())
                                                .then(
                                                        Commands.literal("break")
                                                                .then(
                                                                        Commands.argument(
                                                                                        "amount",
                                                                                        IntegerArgumentType
                                                                                                .integer(1))
                                                                                .executes(
                                                                                        this
                                                                                                ::grantBreak))))
                                                .then(
                                                        Commands.literal("place")
                                                                .then(
                                                                        Commands.argument(
                                                                                        "amount",
                                                                                        IntegerArgumentType
                                                                                                .integer(1))
                                                                                .executes(
                                                                                        this
                                                                                                ::grantPlace))))
                .then(
                        Commands.literal("revoke")
                                .then(
                                        Commands.argument("player", StringArgumentType.word())
                                                .executes(this::revoke)))
                .then(Commands.literal("list").executes(this::list))
                .build();
    }
}
