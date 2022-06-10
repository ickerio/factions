package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SafeCommand implements Command {

    private int run(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Faction faction = User.get(player.getUuid()).getFaction();

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, playerx) -> {
            return GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, faction.getSafe());
        }, Text.of(faction.getColor() + faction.getName() + Formatting.GRAY + "'s Safe")));

        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
                .literal("safe")
                .requires(Requires.multiple(Requires.isMember(), Requires.hasPerms("faction.safe", 0)))
                .executes(this::run)
                .build();
    }
}
