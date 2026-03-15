package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

public class DisbandCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context, boolean confirm)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        if (player == null) {
            return 0;
        }

        User user = Command.getUser(player);
        Faction faction = user.getFaction();
        if (faction == null) return 0;

        if (!faction.getSafe().isEmpty() && !confirm) {
            new Message(Component.translatable("factions.command.disband.fail.safe_not_empty"))
                    .add(
                            new Message(
                                            Component.translatable(
                                                    "factions.command.disband.fail.safe_not_empty.prompt"))
                                    .hover(
                                            Component.translatable(
                                                    "factions.command.disband.fail.safe_not_empty.prompt.hover"))
                                    .click("/f disband confirm")
                                    .format(ChatFormatting.GREEN))
                    .send(player, false);
            return 0;
        }

        NonNullList<ItemStack> safe = faction.clearSafe();

        Containers.dropContents(player.level(), player.blockPosition(), safe);

        new Message(
                        Component.translatable(
                                "factions.command.disband.success", player.getName().getString()))
                .send(faction);
        faction.remove();

        PlayerList manager = source.getServer().getPlayerList();
        for (ServerPlayer p : manager.getPlayers()) {
            manager.sendPlayerPermissionLevel(p);
        }
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("disband")
                .requires(
                        Requires.multiple(
                                Requires.isOwner(), Requires.hasPerms("factions.disband", 0)))
                .executes(context -> this.run(context, false))
                .then(Commands.literal("confirm").executes(context -> this.run(context, true)))
                .build();
    }
}
