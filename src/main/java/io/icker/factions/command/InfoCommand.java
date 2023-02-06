package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.Message;
import io.icker.factions.text.PlainText;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand implements Command {
    private int self(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message().append(new TranslatableText("info.error.factionless").fail()).send(player, false);
            return 0;
        }

        return info(player, user.getFaction());
    }

    private int any(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message().append(new TranslatableText("info.error.not-exist").fail()).send(player, false);
            return 0;
        }

        return info(player, faction);
    }

    public static int info(ServerPlayerEntity player, Faction faction) {
        List<User> users = faction.getUsers();

        UserCache cache = player.getServer().getUserCache();
        String owner = Formatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.OWNER)
                .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        String usersList = users.stream()
            .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
            .collect(Collectors.joining(", "));
        
        String mutualAllies = faction.getMutualAllies().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(Formatting.GRAY + ", "));

        String enemiesWith = Formatting.GRAY + faction.getEnemiesWith().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(Formatting.GRAY + ", "));

        int requiredPower = faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower = users.size() * FactionsMod.CONFIG.POWER.MEMBER + FactionsMod.CONFIG.POWER.BASE;

        // generate the ---
        int numDashes = 32 - faction.getName().length();
        String dashes = new StringBuilder("--------------------------------").substring(0, numDashes/2);

        new Message()
            .append(new PlainText(Formatting.BLACK + dashes + "[ " + faction.getColor() + faction.getName() + Formatting.BLACK + " ]" + dashes))
            .append(new PlainText("\n"))
            .append(new TranslatableText("info.description", faction.getDescription()))
            .append(new TranslatableText("info.owner", owner))
            .append(new TranslatableText("info.members", users.size(), usersList))
            .append(new TranslatableText("info.power", Formatting.GREEN.toString() + faction.getPower() + slash() + requiredPower + slash() + maxPower).hover("info.power.desc"))
            .append(new TranslatableText("info.allies", faction.getMutualAllies().size(), mutualAllies))
            .append(new TranslatableText("info.enemies", faction.getEnemiesWith().size(), enemiesWith))
            .send(player, false);

        return 1;
    }

    private static String slash() {
        return Formatting.GRAY + " / " + Formatting.GREEN;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("info")
            .requires(Requires.hasPerms("factions.info", 0))
            .executes(this::self)
            .then(
                CommandManager.argument("faction", StringArgumentType.greedyString())
                .requires(Requires.hasPerms("factions.info.other", 0))
                .suggests(Suggests.allFactions())
                .executes(this::any)
            )
            .build();
    }
}
