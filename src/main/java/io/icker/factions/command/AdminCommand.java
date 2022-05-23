package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.config.Config;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class AdminCommand implements Command {
    private int bypass(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Member member = Member.get(player.getUuid());
        boolean bypass = !member.isBypassOn();
        member.setBypass(bypass);

        new Message("Successfully toggled claim bypass")
                .filler("·")
                .add(
                        new Message(member.isBypassOn() ? "ON" : "OFF")
                                .format(member.isBypassOn() ? Formatting.GREEN : Formatting.RED))
                .send(player, false);

        return 1;
    }

    private int reload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            FactionsMod.dynmap.reloadAll();
            new Message("Reloaded dynmap marker").send(context.getSource().getPlayer(), false);
        } catch (java.lang.NoClassDefFoundError e) {
            new Message("Dynmap not found").fail().send(context.getSource().getPlayer(), false);
        }

        return 1;
    }

    private int disband(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));

        new Message("An admin disbanded the faction").send(target);
        target.remove();

        new Message("Faction has been removed").send(player, false);

        PlayerManager manager = source.getServer().getPlayerManager();
        for (ServerPlayerEntity p : manager.getPlayerList()) {
            manager.sendCommandTree(p);
        }
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("admin")
            .requires(Requires.hasPerms("factions.admin", Config.REQUIRED_BYPASS_LEVEL))
            .then(
                CommandManager.literal("bypass")
                .requires(Requires.hasPerms("factions.admin.bypass", Config.REQUIRED_BYPASS_LEVEL))
                .executes(this::bypass)
            )
            .then(
                CommandManager.literal("reload")
                .requires(Requires.hasPerms("factions.admin.reload", Config.REQUIRED_BYPASS_LEVEL))
                .executes(this::reload)
            )
            .then(
                CommandManager.literal("disband")
                .requires(Requires.hasPerms("factions.admin.disband", Config.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions())
                    .executes(this::disband)
                )
            )
            .build();
    }
}
