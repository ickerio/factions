package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class CreateCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (!name.matches("^[a-zA-Z0-9_]{2,16}$")){
            new Message("Faction name must be 2-16 characters long, and only contains english letters and numbers!").fail().send(player, false);
            return 0;
        }

        if (Faction.getByName(name) != null) {
            new Message("Cannot create a faction as a one with that name already exists").fail().send(player, false);
            return 0;
        }

        Faction faction = new Faction(name, "No description set", "No faction MOTD set", Formatting.WHITE, false, FactionsMod.CONFIG.BASE_POWER + FactionsMod.CONFIG.MEMBER_POWER, false, 0);
        Faction.add(faction);
        User.get(player.getName().getString()).joinFaction(faction.getID(), User.Rank.OWNER);

        source.getServer().getPlayerManager().sendCommandTree(player);
        new Message("Successfully created faction").send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("create")
            .requires(Requires.multiple(Requires.isFactionless(), Requires.hasPerms("factions.create", 0)))
            .then(
                CommandManager.argument("name", StringArgumentType.greedyString()).executes(this::run)
            )
            .build();
    }
}