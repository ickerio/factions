package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.api.persistents.Member.Rank;
import io.icker.factions.config.Config;
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

        if (Faction.getByName(name) != null) {
            new Message("Cannot create a faction as a one with that name already exists").fail().send(player, false);
            return 0;
        }

        Faction faction = new Faction(name, "No description set", Formatting.WHITE.getName(), false, Config.BASE_POWER + Config.MEMBER_POWER);
        Faction.add(faction);
        Member.get(player.getUuid()).joinFaction(faction.getID(), Rank.OWNER);

        source.getServer().getPlayerManager().sendCommandTree(player);
        new Message("Successfully created faction").send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("create")
            .requires(Requires.isFactionless())
            .requires(Requires.hasPerms("factions.create", 0))
            .then(
                CommandManager.argument("name", StringArgumentType.greedyString()).executes(this::run)
            )
            .build();
    }
}