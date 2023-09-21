package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class SettingsCommand implements Command {
    private int setChat(CommandContext<ServerCommandSource> context, User.ChatMode option)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        User user = User.get(player.getUuid());
        user.chat = option;

        new Message("Successfully set your chat preference").filler("·")
                .add(new Message(user.getChatName()).format(Formatting.BLUE)).send(player, false);

        return 1;
    }

    private int setSounds(CommandContext<ServerCommandSource> context, User.SoundMode option) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        User user = User.get(player.getUuid());
        user.sounds = option;

        new Message("Successfully set your sound preference").filler("·")
                .add(new Message(user.getSoundName()).format(Formatting.BLUE)).send(player, false);

        return 1;
    }

    private int radar(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User config = User.get(player.getUuid());
        boolean radar = !config.radar;
        config.radar = radar;

        new Message("Successfully toggled claim radar").filler("·").add(
                new Message(radar ? "ON" : "OFF").format(radar ? Formatting.GREEN : Formatting.RED))
                .send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("settings")
                .requires(Requires.hasPerms("factions.settings", 0))
                .then(CommandManager.literal("chat")
                        .requires(Requires.hasPerms("factions.settings.chat", 0))
                        .then(CommandManager.literal("global")
                                .executes(context -> setChat(context, User.ChatMode.GLOBAL)))
                        .then(CommandManager.literal("faction")
                                .executes(context -> setChat(context, User.ChatMode.FACTION)))
                        .then(CommandManager.literal("focus")
                                .executes(context -> setChat(context, User.ChatMode.FOCUS))))
                .then(CommandManager.literal("radar")
                        .requires(Requires.hasPerms("factions.settings.radar", 0))
                        .executes(this::radar))
                .then(CommandManager.literal("sounds")
                        .requires(Requires.hasPerms("factions.settings.sounds", 0))
                        .then(CommandManager.literal("none")
                                .executes(context -> setSounds(context, User.SoundMode.NONE)))
                        .then(CommandManager.literal("warnings")
                                .executes(context -> setSounds(context, User.SoundMode.WARNINGS)))
                        .then(CommandManager.literal("faction")
                                .executes(context -> setSounds(context, User.SoundMode.FACTION)))
                        .then(CommandManager.literal("all")
                                .executes(context -> setSounds(context, User.SoundMode.ALL))))
                .build();
    }
}
