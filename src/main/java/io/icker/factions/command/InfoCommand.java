package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;

import java.util.ArrayList;
import java.util.stream.Collectors;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.UserCache;

public class InfoCommand  {
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
		String factionName = StringArgumentType.getString(context, "faction"); // TODO: Suggestions for factions
        
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        Faction faction = Faction.get(factionName);
        if (faction == null) {
            new Message("Faction does not exist").fail().send(player, false);
            return 0;
        }

        return info(player, faction);
    }

    public static int info(ServerPlayerEntity player, Faction faction) {
        ArrayList<Member> members = faction.getMembers();

        String memberText = members.size() + (Config.MAX_FACTION_SIZE != -1 ? "/" + Config.MAX_FACTION_SIZE : (" member" + (members.size() != 1 ? "s" : "")));

        UserCache cache = player.getServer().getUserCache();
		String membersList = members.stream()
			.map(member -> cache.getByUuid(member.uuid).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
			.collect(Collectors.joining(", "));

        int requiredPower = faction.getClaims().size() * Config.CLAIM_WEIGHT;
        int maxPower = Config.BASE_POWER + (members.size() * Config.MEMBER_POWER);

        new Message("")
            .add(
                new Message(memberText)
                .hover(membersList))
            .filler("·")
            .add(
                new Message(Formatting.GREEN.toString() + faction.power + slash() + requiredPower + slash() + maxPower)
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