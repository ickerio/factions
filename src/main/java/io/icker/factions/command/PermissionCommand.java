package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.api.persistents.Relationship.Permissions;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.stream.Collectors;

public class PermissionCommand implements Command {
    private int change(CommandContext<ServerCommandSource> context, boolean add) throws CommandSyntaxException {
        String permissionName = StringArgumentType.getString(context, "permission");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (player == null) return 0;

        Faction sourceFaction = User.get(player.getUuid()).getFaction();
        Faction targetFaction = Faction.getByName(StringArgumentType.getString(context, "faction"));

        if (sourceFaction == null || targetFaction == null) {
            new Message(Text.translatable("factions.command.permissions.change.fail.no_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        Relationship rel = sourceFaction.getRelationship(targetFaction.getID());

        Permissions permission;

        try {
            permission = Permissions.valueOf(permissionName);
        } catch (IllegalArgumentException e) {
            new Message(
                            Text.translatable(
                                    "factions.command.permissions.change.fail.invalid_permission"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if ((!rel.permissions.contains(permission) && !add)
                || (rel.permissions.contains(permission) && add)) {
            new Message(
                            Text.translatable(
                                    "factions.command.permissions.change.fail."
                                            + (rel.permissions.contains(permission)
                                                    ? "already_exists"
                                                    : "doesnt_exist")))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (add) {
            rel.permissions.add(permission);
        } else {
            rel.permissions.remove(permission);
        }

        sourceFaction.setRelationship(rel);

        new Message(Text.translatable("factions.command.permissions.change.success"))
                .send(player, false);
        return 1;
    }

    private int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return change(context, true);
    }

    private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return change(context, false);
    }

    private int changeGuest(CommandContext<ServerCommandSource> context, boolean add) throws CommandSyntaxException {
        String permissionName = StringArgumentType.getString(context, "permission");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (player == null) return 0;

        Faction faction = User.get(player.getUuid()).getFaction();

        if (faction == null) {
            new Message(Text.translatable("factions.command.permissions.guest.fail.no_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        Permissions permission;

        try {
            permission = Permissions.valueOf(permissionName);
        } catch (IllegalArgumentException e) {
            new Message(
                            Text.translatable(
                                    "factions.command.permissions.change.fail.invalid_permission"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if ((!faction.guest_permissions.contains(permission) && !add)
                || (faction.guest_permissions.contains(permission) && add)) {
            new Message(
                            Text.translatable(
                                    "factions.command.permissions.change.fail."
                                            + (faction.guest_permissions.contains(permission)
                                                    ? "already_exists"
                                                    : "doesnt_exist")))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (add) {
            faction.guest_permissions.add(permission);
        } else {
            faction.guest_permissions.remove(permission);
        }

        new Message(Text.translatable("factions.command.permissions.change.success"))
                .send(player, false);
        return 1;
    }

    private int addGuest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return changeGuest(context, true);
    }

    private int removeGuest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return changeGuest(context, false);
    }

    private int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (player == null) return 0;

        Faction sourceFaction = User.get(player.getUuid()).getFaction();
        Faction targetFaction = Faction.getByName(StringArgumentType.getString(context, "faction"));

        if (sourceFaction == null || targetFaction == null) {
            new Message(Text.translatable("factions.command.permissions.change.fail.no_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        String permissionsList =
                sourceFaction.getRelationship(targetFaction.getID()).permissions.stream()
                        .map(Enum::toString)
                        .collect(Collectors.joining(","));

        new Message(
                        Text.translatable(
                                "factions.command.permissions.list.title",
                                Text.literal(targetFaction.getName())
                                        .formatted(targetFaction.getColor())
                                        .formatted(Formatting.BOLD),
                                permissionsList))
                .send(player, false);

        return 1;
    }

    private int listGuest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (player == null) return 0;

        Faction faction = User.get(player.getUuid()).getFaction();

        if (faction == null) {
            new Message(Text.translatable("factions.command.permissions.guest.fail.no_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        String permissionsList =
                faction.guest_permissions.stream()
                        .map(Enum::toString)
                        .collect(Collectors.joining(","));

        new Message(
                        Text.translatable(
                                "factions.command.permissions.list_guest.title", permissionsList))
                .send(player, false);
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("permissions")
                .requires(
                        Requires.multiple(
                                Requires.isLeader(), Requires.hasPerms("factions.permission", 0)))
                .then(
                        CommandManager.literal("add")
                                .requires(Requires.hasPerms("factions.permission.add", 0))
                                .then(
                                        CommandManager.argument(
                                                        "permission", StringArgumentType.word())
                                                .suggests(
                                                        Suggests.enumSuggestion(Permissions.class))
                                                .then(
                                                        CommandManager.literal("faction")
                                                                .requires(
                                                                        Requires.hasPerms(
                                                                                "factions.permission.add.faction",
                                                                                0))
                                                                .then(
                                                                        CommandManager.argument(
                                                                                        "faction",
                                                                                        StringArgumentType
                                                                                                .greedyString())
                                                                                .suggests(
                                                                                        Suggests
                                                                                                .allFactions(
                                                                                                        false))
                                                                                .executes(
                                                                                        this::add)))
                                                .then(
                                                        CommandManager.literal("guest")
                                                                .requires(
                                                                        Requires.hasPerms(
                                                                                "factions.permission.add.guest",
                                                                                0))
                                                                .executes(this::addGuest))))
                .then(
                        CommandManager.literal("remove")
                                .requires(Requires.hasPerms("factions.permission.remove", 0))
                                .then(
                                        CommandManager.argument(
                                                        "permission", StringArgumentType.word())
                                                .suggests(
                                                        Suggests.enumSuggestion(Permissions.class))
                                                .then(
                                                        CommandManager.literal("faction")
                                                                .requires(
                                                                        Requires.hasPerms(
                                                                                "factions.permission.remove.faction",
                                                                                0))
                                                                .then(
                                                                        CommandManager.argument(
                                                                                        "faction",
                                                                                        StringArgumentType
                                                                                                .greedyString())
                                                                                .suggests(
                                                                                        Suggests
                                                                                                .allFactions(
                                                                                                        false))
                                                                                .executes(
                                                                                        this
                                                                                                ::remove)))
                                                .then(
                                                        CommandManager.literal("guest")
                                                                .requires(
                                                                        Requires.hasPerms(
                                                                                "factions.permission.remove.guest",
                                                                                0))
                                                                .executes(this::removeGuest))))
                .then(
                        CommandManager.literal("list")
                                .requires(Requires.hasPerms("factions.permission.list", 0))
                                .then(
                                        CommandManager.literal("faction")
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.permission.list.faction",
                                                                0))
                                                .then(
                                                        CommandManager.argument(
                                                                        "faction",
                                                                        StringArgumentType
                                                                                .greedyString())
                                                                .suggests(
                                                                        Suggests.allFactions(false))
                                                                .executes(this::list)))
                                .then(
                                        CommandManager.literal("guest")
                                                .requires(
                                                        Requires.hasPerms(
                                                                "factions.permission.list.guest",
                                                                0))
                                                .executes(this::listGuest)))
                .build();
    }
}
