package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;

import java.util.ArrayList;
import java.util.stream.Collectors;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;

public class InfoCommand  {
	public static int self(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        Member member = Member.get(player.getUuid());
        if (member == null) {
            source.sendFeedback(new LiteralText("Command can only be whilst in a faction").formatted(Formatting.RED), false);
            return 0;
        }

        source.sendFeedback(buildFactionMessage(member.getFaction(), source), false);
        return 1;
	}

    public static int any(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String factionName = StringArgumentType.getString(context, "faction"); // TODO: Suggestions for factions
        
        ServerCommandSource source = context.getSource();
        
        Faction faction = Faction.get(factionName);
        if (faction == null) {
            source.sendFeedback(new LiteralText("Faction does not exist").formatted(Formatting.RED), false);
            return 0;
        }

        source.sendFeedback(buildFactionMessage(faction, source), false);
        return 1;
	}

    public static MutableText buildFactionMessage(Faction faction, ServerCommandSource source) {
        ArrayList<Member> members = faction.getMembers();

        UserCache cache = source.getMinecraftServer().getUserCache();
		String membersList = members.stream()
			.map(member -> cache.getByUuid(member.uuid).getName())
			.collect(Collectors.joining(", "));

        int requiredPower = faction.getClaimCount() * Config.CLAIM_WEIGHT;
        int maxPower = Config.BASE_POWER + (members.size() * Config.MEMBER_POWER);

        return new LiteralText(false ? "► " : "")
            .append(
                new LiteralText(faction.color.toString() + Formatting.BOLD + faction.name)
                    .styled(s -> s.withHoverEvent(showEvent(faction.description)))
            )
            .append(filler("»"))
            .append(new LiteralText(members.size() + (Config.MAX_FACTION_SIZE != -1 ? "/" + Config.MAX_FACTION_SIZE : " member"))
                .styled(s -> s.withHoverEvent(showEvent(membersList)))
            )
            .append(filler("·"))
            .append(new LiteralText(Formatting.GREEN.toString() + faction.power + slash() + requiredPower + slash() + maxPower)
                .styled(s -> s.withHoverEvent(showEvent("Current / Required / Max")))
            );
    } //☻.★.►·

    private static MutableText filler(String symbol) {
        return new LiteralText(" " + Formatting.RESET + Formatting.DARK_GRAY + symbol +  Formatting.RESET +  " ");
    }

    private static HoverEvent showEvent(String text) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(text));
    }

    private static String slash() {
        return Formatting.GRAY + " / " + Formatting.GREEN;
    }
}