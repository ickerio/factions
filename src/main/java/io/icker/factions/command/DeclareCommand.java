package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.Date;
import java.util.Locale;

public class DeclareCommand implements Command {
    private int improve(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return updateRelationship(context, 1);
    }
    private int insult(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return updateRelationship(context, -1);
    }

    private int updateRelationship(CommandContext<ServerCommandSource> context, int points) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "faction");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction targetFaction = Faction.getByName(name);

        if (targetFaction == null) {
            new Message("Cannot change faction relationship with a faction that doesn't exist").fail().send(player, false);
            return 0;
        }
        
        Faction sourceFaction = User.get(player.getName().getString()).getFaction();

        if (sourceFaction.equals(targetFaction)) {
            new Message("Cannot use the declare command on your own faction").fail().send(player, false);
            return 0;
        }

        if(targetFaction.isAdmin()){
            new Message("Cannot declare anything on admin-protected factions!").fail().send(player, false);
            return 0;
        }
        Date nextUpdate = new Date(sourceFaction.relationsLastUpdate + (FactionsMod.CONFIG.HOURS_BEFORE_NEXT_FABRICATE * 1000 * 3600));

        boolean isAfter = nextUpdate.before(new Date());
        if(!isAfter) {
            new Message("Cannot improve or harsh relationships with anyone, until " + nextUpdate.toString()).fail().send(player, false);
            return 0;
        }

        int warTaxes = FactionsMod.CONFIG.FABRICATE_TAXES;

        sourceFaction.adjustPower(-warTaxes);

        Relationship rel = sourceFaction.getRelationship(targetFaction.getID());
        Relationship rev = targetFaction.getRelationship(sourceFaction.getID());
        rel = new Relationship(targetFaction.getID(), rel.points + points);
        sourceFaction.setRelationship(rel);

        sourceFaction.relationsLastUpdate = new Date().getTime();


        String msgStatus = rel.status.name().toLowerCase(Locale.ROOT);

        if (rel.status == rev.status) {
            new Message("You are now mutually ").add(msgStatus).add(" with " + targetFaction.getName()).send(sourceFaction);
            new Message("You are now mutually ").add(msgStatus).add(" with " + sourceFaction.getName()).send(targetFaction);
            return 1;
        }

        new Message("You have declared " + targetFaction.getName() + " as ").add(msgStatus).add("; Your points of relationships are now: " + rel.points).send(sourceFaction);

        if(rel.status.equals(Relationship.Status.ENEMY)){
            long dateofwar = new Date(new Date().getTime() + (1000 * 3600 * 24 * 3)).getTime();

            sourceFaction.relationsLastUpdate = dateofwar;
            targetFaction.relationsLastUpdate = dateofwar;
            rev = new Relationship(sourceFaction.getID(), -FactionsMod.CONFIG.DAYS_TO_FABRICATE-1);
            targetFaction.setRelationship(rev);
            new Message("§4There will be blood...").sendToGlobalChat();
            new Message("§4The §r" +sourceFaction.getColor() + sourceFaction.getName() + "§4 declares war on §r" + targetFaction.getColor() + targetFaction.getName() + "§4!").sendToGlobalChat();
        }
      
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("declare")
            .requires(Requires.isLeader())
            .then(
                CommandManager.literal("improve")
                .requires(Requires.hasPerms("factions.declare.ally", 0))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions(false))
                    .executes(this::improve)
                )
            )
            .then(
                CommandManager.literal("insult")
                .requires(Requires.hasPerms("factions.declare.enemy", 0))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions(false))
                    .executes(this::insult)
                )
            )
            .build();
    }
    
}
