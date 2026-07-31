package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.core.TradeRequestManager;
import io.icker.factions.core.TradeRequestManager.TradeRequest;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class TradeCommand implements Command {
    private int request(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        User sourceUser = Command.getUser(player);

        if (!sourceUser.isInFaction()) {
            new Message(Component.translatable("factions.command.trade.fail.not_member"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        Faction sourceFaction = sourceUser.getFaction();
        String factionName = StringArgumentType.getString(context, "faction");
        Faction targetFaction = Faction.getByName(factionName);

        if (targetFaction == null) {
            new Message(Component.translatable("factions.command.trade.fail.nonexistent_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (targetFaction.getID().equals(sourceFaction.getID())) {
            new Message(Component.translatable("factions.command.trade.fail.own_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        long now = System.currentTimeMillis();
        TradeRequestManager.request(
                targetFaction.getID(), sourceFaction.getID(), player.getUUID(), now);

        new Message(
                        Component.translatable(
                                "factions.command.trade.request.received",
                                sourceFaction.getName(),
                                FactionsMod.CONFIG.GATHER.REQUEST_EXPIRY_SECONDS))
                .hover(Component.translatable("factions.command.trade.request.hover"))
                .click("/factions trade y")
                .send(targetFaction);

        new Message(
                        Component.translatable(
                                "factions.command.trade.request.sent",
                                targetFaction.getName()))
                .send(player, false);
        return 1;
    }

    private int accept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer responder = context.getSource().getPlayerOrException();
        User responderUser = Command.getUser(responder);

        if (!responderUser.isInFaction()) {
            new Message(Component.translatable("factions.command.trade.fail.not_member"))
                    .fail()
                    .send(responder, false);
            return 0;
        }

        Faction responderFaction = responderUser.getFaction();
        UUID responderFactionID = responderFaction.getID();

        TradeRequest req = TradeRequestManager.peek(responderFactionID);
        if (req == null) {
            new Message(Component.translatable("factions.command.trade.accept.fail.no_request"))
                    .fail()
                    .send(responder, false);
            return 0;
        }

        long now = System.currentTimeMillis();
        int expirySeconds = FactionsMod.CONFIG.GATHER.REQUEST_EXPIRY_SECONDS;
        if (TradeRequestManager.isExpired(req, now, expirySeconds)) {
            TradeRequestManager.consume(responderFactionID);
            new Message(Component.translatable("factions.command.trade.accept.fail.expired"))
                    .fail()
                    .send(responder, false);
            return 0;
        }

        TradeRequestManager.consume(responderFactionID);

        ServerPlayer requester =
                context.getSource().getServer().getPlayerList().getPlayer(req.requesterPlayerID());

        GatherCommand gather = new GatherCommand();
        gather.execGo(responder);
        if (requester != null) {
            gather.execGo(requester);
        }

        new Message(Component.translatable("factions.command.trade.accept.success"))
                .send(responder, false);
        if (requester != null) {
            new Message(Component.translatable("factions.command.trade.accept.success"))
                    .send(requester, false);
        }
        return 1;
    }

    private int deny(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer responder = context.getSource().getPlayerOrException();
        User responderUser = Command.getUser(responder);

        if (!responderUser.isInFaction()) {
            new Message(Component.translatable("factions.command.trade.fail.not_member"))
                    .fail()
                    .send(responder, false);
            return 0;
        }

        Faction responderFaction = responderUser.getFaction();
        UUID responderFactionID = responderFaction.getID();

        TradeRequest req = TradeRequestManager.peek(responderFactionID);
        if (req == null) {
            new Message(Component.translatable("factions.command.trade.deny.fail.no_request"))
                    .fail()
                    .send(responder, false);
            return 0;
        }

        TradeRequestManager.deny(responderFactionID);

        new Message(Component.translatable("factions.command.trade.deny.success"))
                .send(responder, false);

        Faction requesterFaction = Faction.get(req.requesterFactionID());
        if (requesterFaction != null) {
            new Message(
                            Component.translatable(
                                    "factions.command.trade.deny.notified",
                                    responderFaction.getName()))
                    .fail()
                    .send(requesterFaction);
        }
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("trade")
                .requires(
                        Requires.multiple(
                                Requires.isMember(),
                                s -> FactionsMod.CONFIG.GATHER != null
                                        && FactionsMod.CONFIG.GATHER.ENABLED,
                                Requires.hasPerms("factions.trade", 0)))
                .then(Commands.literal("y").executes(this::accept))
                .then(Commands.literal("n").executes(this::deny))
                .then(
                        Commands.argument("faction", StringArgumentType.greedyString())
                                .suggests(Suggests.allFactions(false))
                                .executes(this::request))
                .build();
    }
}
