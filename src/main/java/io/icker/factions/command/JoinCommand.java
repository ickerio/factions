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
import net.minecraft.text.Text;

public class JoinCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        Faction faction = Faction.getByName(name);

        if (faction == null) {
            new Message(Text.translatable("factions.command.join.fail.nonexistent_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        boolean invited = faction.isInvited(player.getUuid());

        if (!faction.isOpen() && !invited) {
            new Message(Text.translatable("factions.command.join.fail.private_no_invite"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.MAX_FACTION_SIZE != -1
                && faction.getUsers().size() >= FactionsMod.CONFIG.MAX_FACTION_SIZE) {
            new Message(Text.translatable("factions.command.join.fail.faction_full"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (invited) faction.invites.remove(player.getUuid());
        Command.getUser(player).joinFaction(faction.getID(), User.Rank.MEMBER);
        source.getServer().getPlayerManager().sendCommandTree(player);

        new Message(
                        Text.translatable(
                                "factions.command.join.success", player.getName().getString()))
                .send(faction);
        faction.adjustPower(FactionsMod.CONFIG.POWER.MEMBER);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("join")
                .requires(
                        Requires.multiple(
                                Requires.isFactionless(), Requires.hasPerms("factions.join", 0)))
                .then(
                        CommandManager.argument("name", StringArgumentType.greedyString())
                                .suggests(Suggests.openInvitedFactions())
                                .executes(this::run))
                .build();
    }
}
