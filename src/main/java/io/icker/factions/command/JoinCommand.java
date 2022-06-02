package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Invite;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class JoinCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Faction.getByName(name);

        if (faction == null) {
            new Message("Cannot join faction as none exist with that name").fail().send(player, false);
            return 0;
        }

        Invite invite = Invite.get(player.getUuid(), faction.getID());
        if (!faction.isOpen() && invite == null) {
            new Message("Cannot join faction as it is not open and you are not invited").fail().send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.MAX_FACTION_SIZE != -1 && faction.getUsers().size() >= FactionsMod.CONFIG.MAX_FACTION_SIZE) {
            new Message("Cannot join faction as it is currently full").fail().send(player, false);
            return 0;
        }

        if (invite != null) invite.remove();
        User.get(player.getUuid()).joinFaction(faction.getID(), Rank.MEMBER);
        source.getServer().getPlayerManager().sendCommandTree(player);

        new Message(player.getName().getString() + " joined").send(faction);
        faction.adjustPower(FactionsMod.CONFIG.MEMBER_POWER);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("join")
            .requires(Requires.hasPerms("factions.join", 0))
            .requires(Requires.isFactionless())
            .then(
                CommandManager.argument("name", StringArgumentType.greedyString())
                .suggests(Suggests.openFactions())
                .executes(this::run)
            )
            .build();
    }
}