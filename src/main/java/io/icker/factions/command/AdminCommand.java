package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import io.icker.factions.database.Faction;
import io.icker.factions.database.PlayerConfig;
import io.icker.factions.FactionsMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.PlayerManager;

public class AdminCommand {
    public static int bypass(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        PlayerConfig config = PlayerConfig.get(player.getUuid());
        boolean bypass = !config.bypass;
        config.setBypass(bypass);

        new Message("Successfully toggled claim bypass")
            .filler("·")
            .add(
                new Message(bypass ? "ON" : "OFF")
                    .format(bypass ? Formatting.GREEN : Formatting.RED))
            .send(player, false);

        return 1;
    }
    
    public static int reload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            FactionsMod.dynmap.reloadAll();
            new Message("Reloaded dynmap marker").send(context.getSource().getPlayer(), false);
        } catch (java.lang.NoClassDefFoundError e) {
            new Message("Dynmap not found").fail().send(context.getSource().getPlayer(), false);
        }

        return 1;
    }

    public static int disband(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction target = Faction.get(StringArgumentType.getString(context, "faction"));

        new Message("An admin disbanded the faction").send(target);
        target.remove();

        new Message("Faction has been removed").send(player, false);

        PlayerManager manager = source.getServer().getPlayerManager();
        for (ServerPlayerEntity p : manager.getPlayerList()) {
           manager.sendCommandTree(p);
        }
        return 1;
    }
}
