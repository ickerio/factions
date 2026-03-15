package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.ui.ListGui;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ListCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        User user = User.get(player.getUUID());

        if (FactionsMod.CONFIG.GUI) {
            new ListGui(player, user, null);
            return 1;
        }

        Collection<Faction> factions = Faction.all();
        int size = factions.size();

        new Message(Component.translatable("factions.gui.list.title")).send(player, false);

        if (size == 0) return 1;

        Message list = new Message();
        for (Faction faction : factions) {
            String name = faction.getName();
            list.add(new Message(name).click("/factions info " + name).format(faction.getColor()))
                    .add(", ");
        }

        list.send(player, false);

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("list")
                .requires(Requires.hasPerms("factions.list", 0))
                .executes(this::run)
                .build();
    }
}
