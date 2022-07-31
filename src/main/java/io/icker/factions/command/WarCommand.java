package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.core.WarManager;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static io.icker.factions.api.persistents.Relationship.Status.WARRING;

public class WarCommand implements Command {
    private int declare (CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "faction");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction targetFaction = Faction.getByName(name);

        if (targetFaction == null) {
            new Message("Cannot go to war with a nonexistent faction").fail().send(player, false);
            return 0;
        }

        Faction sourceFaction = Command.getUser(player).getFaction();

        if (sourceFaction.equals(targetFaction)) {
            new Message("Cannot declare war on yourself").fail().send(player, false);
            return 0;
        }

        if (sourceFaction.getRelationship(targetFaction.getID()).status == WARRING) {
            new Message("You are already at war with that faction").fail().send(player, false);
            return 0;
        }

        if (!WarManager.eligibleForWar(sourceFaction, targetFaction)) {
            new Message("You cannot currently go to war with that faction").fail().send(player, false);
            return 0;
        }

        Relationship rel = new Relationship(targetFaction.getID(), WARRING);
        sourceFaction.setRelationship(rel);

        new Message("You have declared war on " + targetFaction.getName()).send(sourceFaction);

        new Message(sourceFaction.getName() + " have declared war on you")
            .hover("Click to declare war back")
            .click(String.format("/factions war declare %s", sourceFaction.getName()))
            .send(targetFaction);

        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("war")
            .requires(Requires.isLeader())
            .then(
                CommandManager.literal("declare")
                .requires(WarManager::eligibleForWar)
                    .then(
                        CommandManager.argument("faction", StringArgumentType.greedyString())
                        .suggests(Suggests.eligibleForWar())
                        .executes(this::declare)
                    )
            ).build();
    }
}
