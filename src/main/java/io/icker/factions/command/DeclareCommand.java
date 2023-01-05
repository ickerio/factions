package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.events.RelationshipEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.text.Message;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

public class DeclareCommand implements Command {
    private int ally(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.ALLY);
    }

    private int neutral(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.NEUTRAL);
    }

    private int enemy(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.ENEMY);
    }

    private int updateRelationship(CommandContext<ServerCommandSource> context, Relationship.Status status) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "faction");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction targetFaction = Faction.getByName(name);

        if (targetFaction == null) {
            new Message().append(new TranslatableText("declare.error.exist").fail()).send(player, false);
            return 0;
        }
        
        Faction sourceFaction = Command.getUser(player).getFaction();

        if (sourceFaction.equals(targetFaction)) {
            new Message().append(new TranslatableText("declare.error.own").fail()).send(player, false);
            return 0;
        }

        if (sourceFaction.getRelationship(targetFaction.getID()).status == status) {
            new Message().append(new TranslatableText("declare.error.already").fail()).send(player, false);
            return 0;
        }

        Relationship.Status mutual = null;

        if (sourceFaction.getRelationship(targetFaction.getID()).status == targetFaction.getRelationship(sourceFaction.getID()).status) {
            mutual = sourceFaction.getRelationship(targetFaction.getID()).status;
        }

        Relationship rel = new Relationship(targetFaction.getID(), status);
        Relationship rev = targetFaction.getRelationship(sourceFaction.getID());
        sourceFaction.setRelationship(rel);

        RelationshipEvents.NEW_DECLARATION.invoker().onNewDecleration(rel);

        TranslatableText msgStatus = rel.status == Relationship.Status.ALLY ? new TranslatableText("relationship.ally")
        : rel.status == Relationship.Status.ENEMY ? new TranslatableText("relationship.enemy")
        : new TranslatableText("relationship.neutral");

        if (rel.status == rev.status) {
            RelationshipEvents.NEW_MUTUAL.invoker().onNewMutual(rel);
            new Message()
                    .append(new TranslatableText("declare.notification.mutual"))
                    .append(msgStatus)
                    .append(new TranslatableText("declare.notification.mutual.with", targetFaction.getName()))
                    .send(sourceFaction);

            new Message()
                    .append(new TranslatableText("declare.notification.mutual"))
                    .append(msgStatus)
                    .append(new TranslatableText("declare.notification.mutual.with", sourceFaction.getName()))
                    .send(targetFaction);
            return 1;
        } else if (mutual != null) {
            RelationshipEvents.END_MUTUAL.invoker().onEndMutual(rel, mutual);
        }

        new Message()
                .append(new TranslatableText("declare.notification", targetFaction.getName())).append(msgStatus).send(sourceFaction);

        if (rel.status != Relationship.Status.NEUTRAL)
            new Message()
                .append(
                        new TranslatableText("declare.notification.target", sourceFaction.getName())
                            .hover("declare.click")
                            .click(String.format("/factions declare %s %s", rel.status.toString().toLowerCase(Locale.ROOT), sourceFaction.getName()))
                )
                .append(
                        msgStatus
                            .hover("declare.click")
                            .click(String.format("/factions declare %s %s", rel.status.toString().toLowerCase(Locale.ROOT), sourceFaction.getName()))
                )
                .send(targetFaction);
      
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("declare")
            .requires(Requires.isLeader())
            .then(
                CommandManager.literal("ally")
                .requires(Requires.hasPerms("factions.declare.ally", 0))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions(false))
                    .executes(this::ally)
                )
            )
            .then(
                CommandManager.literal("neutral")
                .requires(Requires.hasPerms("factions.declare.neutral", 0))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions(false))
                    .executes(this::neutral)
                )
            )
            .then(
                CommandManager.literal("enemy")
                .requires(Requires.hasPerms("factions.declare.enemy", 0))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions(false))
                    .executes(this::enemy)
                )
            )
            .build();
    }
    
}
