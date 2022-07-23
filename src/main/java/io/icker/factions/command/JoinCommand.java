package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static io.icker.factions.FactionsMod.CONFIG;
import static io.icker.factions.api.persistents.User.Rank.MEMBER;

public class JoinCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        Faction faction = Faction.getByName(name);

        if (faction == null) {
            new Message("Cannot join faction as none exist with that name").fail().send(player, false);
            return 0;
        }

        boolean invited = faction.isInvited(player.getUuid());

        if (!faction.isOpen() && !invited) {
            new Message("Cannot join faction as it is not open and you are not invited").fail().send(player, false);
            return 0;
        }

        if (CONFIG.MAX_FACTION_SIZE != -1 && faction.getUsers().size() >= CONFIG.MAX_FACTION_SIZE) {
            new Message("Cannot join faction as it is currently full").fail().send(player, false);
            return 0;
        }

        if (invited) faction.invites.remove(player.getUuid());
        Command.getUser(player).joinFaction(faction.getID(), MEMBER);
        source.getServer().getPlayerManager().sendCommandTree(player);

        new Message(player.getName().getString() + " joined").send(faction);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("join")
            .requires(Requires.multiple(Requires.isFactionless(), Requires.hasPerms("factions.join", 0)))
            .then(
                CommandManager.argument("name", StringArgumentType.greedyString())
                .suggests(Suggests.openInvitedFactions())
                .executes(this::run)
            )
            .build();
    }
}