package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.config.Config;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand {
    public static int self(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Member member = Member.get(player.getUuid());
        if (member == null) {
            new Message("Command can only be used whilst in a faction").fail().send(player, false);
            return 0;
        }

        return info(player, member.getFaction());
    }

    public static int any(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message("Faction does not exist").fail().send(player, false);
            return 0;
        }

        return info(player, faction);
    }

    public static int info(ServerPlayerEntity player, Faction faction) {
        List<Member> members = faction.getMembers();

        //ArrayList<Ally> allies = Ally.getAllies(faction.name);

        String memberText = members.size() + (Config.MAX_FACTION_SIZE != -1 ? "/" + Config.MAX_FACTION_SIZE : (" member" + (members.size() != 1 ? "s" : "")));
        //String allyText = allies.size() + (allies.size() != 1 ? " allies" : " ally");

        UserCache cache = player.getServer().getUserCache();
        String membersList = members.stream()
                .map(member -> cache.getByUuid(member.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        //String alliesList = allies.stream()
        //        .map(ally -> ally.target)
        //        .collect(Collectors.joining(", "));

        int requiredPower = faction.getClaims().size() * Config.CLAIM_WEIGHT;
        int maxPower = members.size() * Config.MEMBER_POWER + Config.BASE_POWER;

        new Message("")
                .add(
                        new Message(memberText)
                                .hover(membersList))
                .filler("·")
                .add("")
                        //new Message(allyText)
                        //        .hover(alliesList))
                .filler("·")
                .add(
                        new Message(Formatting.GREEN.toString() + faction.getPower() + slash() + requiredPower + slash() + maxPower)
                                .hover("Current / Required / Max")
                )
                .prependFaction(faction)
                .send(player, false);

        return 1;
    }

    private static String slash() {
        return Formatting.GRAY + " / " + Formatting.GREEN;
    }
}