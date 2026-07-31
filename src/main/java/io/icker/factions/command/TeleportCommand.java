package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.User;
import io.icker.factions.core.TeleportRequestManager;
import io.icker.factions.core.TeleportRequestManager.Request;
import io.icker.factions.mixin.CombatTrackerAccessor;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.UUID;

public class TeleportCommand implements Command {
    private int request(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer requester = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "player");
        ServerPlayer target =
                context.getSource().getServer().getPlayerList().getPlayerByName(name);

        if (target == null) {
            new Message(Component.translatable("factions.command.tp.fail.offline"))
                    .fail()
                    .send(requester, false);
            return 0;
        }

        if (target.getUUID().equals(requester.getUUID())) {
            new Message(Component.translatable("factions.command.tp.fail.self"))
                    .fail()
                    .send(requester, false);
            return 0;
        }

        User requesterUser = Command.getUser(requester);
        User targetUser = Command.getUser(target);
        if (!requesterUser.isInFaction()
                || !targetUser.isInFaction()
                || !requesterUser.getFaction().getID().equals(targetUser.getFaction().getID())) {
            new Message(Component.translatable("factions.command.tp.fail.not_member"))
                    .fail()
                    .send(requester, false);
            return 0;
        }

        long now = System.currentTimeMillis();
        TeleportRequestManager.request(target.getUUID(), requester.getUUID(), now);

        new Message(
                        Component.translatable(
                                "factions.command.tp.request.sent",
                                target.getName().getString()))
                .send(requester, false);
        new Message(
                        Component.translatable(
                                "factions.command.tp.request.received",
                                requester.getName().getString(),
                                FactionsMod.CONFIG.TELEPORT.REQUEST_EXPIRY_SECONDS))
                .hover(Component.translatable("factions.command.tp.request.hover"))
                .click("/factions tp accept")
                .send(target, false);
        return 1;
    }

    private int accept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        UUID targetID = target.getUUID();

        Request req = TeleportRequestManager.peek(targetID);
        if (req == null) {
            new Message(Component.translatable("factions.command.tp.accept.fail.no_request"))
                    .fail()
                    .send(target, false);
            return 0;
        }

        long now = System.currentTimeMillis();
        int expirySeconds = FactionsMod.CONFIG.TELEPORT.REQUEST_EXPIRY_SECONDS;
        if (TeleportRequestManager.isExpired(req, now, expirySeconds)) {
            TeleportRequestManager.deny(targetID);
            new Message(Component.translatable("factions.command.tp.accept.fail.expired"))
                    .fail()
                    .send(target, false);
            return 0;
        }

        UUID requesterID = req.requesterID();
        ServerPlayer requester =
                context.getSource().getServer().getPlayerList().getPlayer(requesterID);
        if (requester == null) {
            TeleportRequestManager.consume(targetID);
            new Message(Component.translatable("factions.command.tp.fail.offline"))
                    .fail()
                    .send(target, false);
            return 0;
        }

        User targetUser = Command.getUser(target);
        User requesterUser = Command.getUser(requester);
        if (!targetUser.isInFaction()
                || !requesterUser.isInFaction()
                || !targetUser.getFaction().getID().equals(requesterUser.getFaction().getID())) {
            TeleportRequestManager.consume(targetID);
            new Message(Component.translatable("factions.command.tp.fail.not_member"))
                    .fail()
                    .send(target, false);
            return 0;
        }

        int cooldownSeconds = FactionsMod.CONFIG.TELEPORT.COOLDOWN_SECONDS;
        if (TeleportRequestManager.isOnCooldown(requesterID, now, cooldownSeconds)) {
            long remainingMs =
                    TeleportRequestManager.cooldownRemainingMillis(
                            requesterID, now, cooldownSeconds);
            long remainingSeconds = (remainingMs + 999) / 1000;
            Message cooldownMsg =
                    new Message(
                                    Component.translatable(
                                            "factions.command.tp.fail.cooldown", remainingSeconds))
                            .fail();
            cooldownMsg.send(target, false);
            cooldownMsg.send(requester, false);
            return 0;
        }

        int lastDamageTime =
                ((CombatTrackerAccessor) requester.getCombatTracker()).getLastDamageTime();
        if (lastDamageTime != 0
                && requester.tickCount - lastDamageTime
                        <= FactionsMod.CONFIG.TELEPORT.DAMAGE_COOLDOWN) {
            Message combatMsg =
                    new Message(Component.translatable("factions.command.tp.fail.combat")).fail();
            combatMsg.send(target, false);
            combatMsg.send(requester, false);
            return 0;
        }

        TeleportRequestManager.consume(targetID);
        TeleportRequestManager.markTeleported(requesterID, now);

        ServerLevel targetLevel = (ServerLevel) target.level();
        requester.teleportTo(
                targetLevel,
                target.getX(),
                target.getY(),
                target.getZ(),
                new HashSet<>(),
                target.getYHeadRot(),
                target.getXRot(),
                false);

        new Message(
                        Component.translatable(
                                "factions.command.tp.accept.success.requester",
                                target.getName().getString()))
                .send(requester, false);
        new Message(
                        Component.translatable(
                                "factions.command.tp.accept.success.target",
                                requester.getName().getString()))
                .send(target, false);
        return 1;
    }

    private int deny(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        UUID targetID = target.getUUID();

        Request req = TeleportRequestManager.peek(targetID);
        if (req == null) {
            new Message(Component.translatable("factions.command.tp.deny.fail.no_request"))
                    .fail()
                    .send(target, false);
            return 0;
        }

        TeleportRequestManager.deny(targetID);

        new Message(Component.translatable("factions.command.tp.deny.success"))
                .send(target, false);

        ServerPlayer requester =
                context.getSource().getServer().getPlayerList().getPlayer(req.requesterID());
        if (requester != null) {
            new Message(
                            Component.translatable(
                                    "factions.command.tp.deny.notified",
                                    target.getName().getString()))
                    .fail()
                    .send(requester, false);
        }
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("tp")
                .requires(
                        Requires.multiple(
                                Requires.isMember(),
                                s ->
                                        FactionsMod.CONFIG.TELEPORT != null
                                                && FactionsMod.CONFIG.TELEPORT.ENABLED,
                                Requires.hasPerms("factions.tp", 0)))
                .then(Commands.literal("accept").executes(this::accept))
                .then(Commands.literal("deny").executes(this::deny))
                .then(
                        Commands.argument("player", StringArgumentType.word())
                                .suggests(Suggests.onlineFactionMembersButYou())
                                .executes(this::request))
                .build();
    }
}
