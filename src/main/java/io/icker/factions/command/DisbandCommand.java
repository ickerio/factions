package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;

public class DisbandCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context, boolean confirm)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (player == null) {
            return 0;
        }

        User user = Command.getUser(player);
        Faction faction = user.getFaction();
        if (faction == null) return 0;

        if (!faction.getSafe().isEmpty() && !confirm) {
            new Message(Text.translatable("factions.command.disband.fail.safe_not_empty"))
                    .add(
                            new Message(
                                            Text.translatable(
                                                    "factions.command.disband.fail.safe_not_empty.prompt"))
                                    .hover(
                                            Text.translatable(
                                                    "factions.command.disband.fail.safe_not_empty.prompt.hover"))
                                    .click("/f disband confirm")
                                    .format(Formatting.GREEN))
                    .send(player, false);
            return 0;
        }

        DefaultedList<ItemStack> safe = faction.clearSafe();

        ItemScatterer.spawn(player.getWorld(), player.getBlockPos(), safe);

        new Message(
                        Text.translatable(
                                "factions.command.disband.success", player.getName().getString()))
                .send(faction);
        faction.remove();

        PlayerManager manager = source.getServer().getPlayerManager();
        for (ServerPlayerEntity p : manager.getPlayerList()) {
            manager.sendCommandTree(p);
        }
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("disband")
                .requires(
                        Requires.multiple(
                                Requires.isOwner(), Requires.hasPerms("factions.disband", 0)))
                .executes(context -> this.run(context, false))
                .then(
                        CommandManager.literal("confirm")
                                .executes(context -> this.run(context, true)))
                .build();
    }
}
