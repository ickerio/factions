package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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

public class PermissionCommand implements Command {
    private int change(CommandContext<ServerCommandSource> context, boolean add) {
        String permissionName = StringArgumentType.getString(context, "permission");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) return 0;

        Faction sourceFaction = User.get(player.getUuid()).getFaction();
        Faction targetFaction = Faction.getByName(StringArgumentType.getString(context, "faction"));

        if (sourceFaction == null || targetFaction == null) {
            new Message("You must be in a faction and you must provide a valid function").fail().send(player, false);
            return 0;
        }

        Relationship rel = sourceFaction.getRelationship(targetFaction.getID());

        Permissions permission;

        try {
            permission = Permissions.valueOf(permissionName);
        } catch (IllegalArgumentException e) {
            new Message("Not a valid permission").fail().send(player, false);
            return 0;
        }

        if ((!rel.permissions.contains(permission) && !add) || (rel.permissions.contains(permission) && add)) {
            new Message(String.format("Could not change because the permission %s", rel.permissions.contains(permission) ? "already exists" : "doesn't exist")).fail().send(player, false);
            return 0;
        }

        if (add) {
            rel.permissions.add(permission);
        } else {
            rel.permissions.remove(permission);
        }

        sourceFaction.setRelationship(rel);

        new Message("Successfully changed permissions").send(player, false);
        return 1;
    }

    private int add(CommandContext<ServerCommandSource> context) {
        return change(context, true);
    }

    private int remove(CommandContext<ServerCommandSource> context) {
        return change(context, false);
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("permissions")
            .requires(Requires.multiple(Requires.isLeader(), Requires.hasPerms("factions.permission", 0)))
            .then(
                CommandManager.literal("add")
                    .requires(Requires.hasPerms("factions.permission.add", 0))
                    .then(
                        CommandManager.argument("permission", StringArgumentType.word())
                            .suggests(Suggests.enumSuggestion(Permissions.class))
                            .then(
                                CommandManager.argument("faction", StringArgumentType.greedyString())
                                    .suggests(Suggests.allFactions(false))
                                    .executes(this::add)
                            )
                    )
            )
            .then(
                CommandManager.literal("remove")
                    .requires(Requires.hasPerms("factions.permission.remove", 0))
                    .then(
                        CommandManager.argument("permission", StringArgumentType.word())
                            .suggests(Suggests.enumSuggestion(Permissions.class))
                            .then(
                                CommandManager.argument("faction", StringArgumentType.greedyString())
                                    .suggests(Suggests.allFactions(false))
                                    .executes(this::remove)
                            )
                    )
            )
            .build();
    }
}
