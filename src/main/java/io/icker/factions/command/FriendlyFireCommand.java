package io.icker.factions.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FriendlyFireCommand implements Command {
    private int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        Faction faction = Command.getUser(player).getFaction();

        faction.setFriendlyFire(enabled);
        
        new Message(
                        Text.translatable(
                                enabled
                                        ? "factions.command.friendlyfire.success.enabled"
                                        : "factions.command.friendlyfire.success.disabled"))
                .format(enabled ? Formatting.GREEN : Formatting.RED)
                .prependFaction(faction)
                .send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("friendlyfire")
                .requires(
                        Requires.multiple(
                                Requires.hasPerms("factions.friendlyfire.toggle", 0),
                                Requires.isLeader()))
                .then(
                        CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(this::execute))
                .build();
    }
}