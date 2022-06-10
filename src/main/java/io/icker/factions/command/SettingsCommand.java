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

public class SettingsCommand implements Command{
    private int globalChat(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return setChat(context.getSource(), User.ChatMode.GLOBAL);
    }

    private int factionChat(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return setChat(context.getSource(), User.ChatMode.FACTION);
    }

    private int focusChat(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return setChat(context.getSource(), User.ChatMode.FOCUS);
    }

    private int setChat(ServerCommandSource source, User.ChatMode option) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        User user = User.get(player.getUuid());
        user.chat = option;   

        new Message("Successfully set your chat preference")
            .filler("·")
            .add(
                new Message(user.getChatName())
                    .format(Formatting.BLUE)
            )
            .send(player, false);

        return 1;
    }

    private int noSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return setSounds(context.getSource(), User.SoundMode.NONE);
    }

    private int warningSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return setSounds(context.getSource(), User.SoundMode.WARNINGS);
    }

    private int factionSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return setSounds(context.getSource(), User.SoundMode.FACTION);
    }

    private int allSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return setSounds(context.getSource(), User.SoundMode.ALL);
    }

    private int setSounds(ServerCommandSource source, User.SoundMode option) {
        ServerPlayerEntity player = source.getPlayer();
        User user = User.get(player.getUuid());
        user.sounds = option;   

        new Message("Successfully set your sound preference")
            .filler("·")
            .add(
                new Message(user.getSoundName())
                    .format(Formatting.BLUE)
            )
            .send(player, false);
            
        return 1;
    }

    private int radar(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User config = User.get(player.getUuid());
        boolean radar = !config.radar;
        config.radar = radar;

        new Message("Successfully toggled claim radar")
            .filler("·")
            .add(
                new Message(radar ? "ON" : "OFF")
                    .format(radar ? Formatting.GREEN : Formatting.RED)
            )
            .send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("settings")
            .requires(Requires.hasPerms("factions.settings", 0))
            .then(
                CommandManager.literal("chat")
                .requires(Requires.hasPerms("factions.settings.chat", 0))
                .then(CommandManager.literal("global").executes(this::globalChat))
                .then(CommandManager.literal("faction").executes(this::factionChat))
                .then(CommandManager.literal("focus").executes(this::focusChat))
            )
            .then(
                CommandManager.literal("radar")
                .requires(Requires.hasPerms("factions.settings.radar", 0))
                .executes(this::radar)
            )
            .then(
                CommandManager.literal("sounds")
                .requires(Requires.hasPerms("factions.settings.sounds", 0))
                .then(CommandManager.literal("none").executes(this::noSounds))
                .then(CommandManager.literal("warnings").executes(this::warningSounds))
                .then(CommandManager.literal("faction").executes(this::factionSounds))
                .then(CommandManager.literal("all").executes(this::allSounds))
            )
            .build();
    }
}
