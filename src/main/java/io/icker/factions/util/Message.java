package io.icker.factions.util;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

public class Message {
    public static PlayerList manager;
    private MutableComponent text;

    public Message() {
        text = Component.literal("");
    }

    public Message(String message) {
        text = (MutableComponent) Component.nullToEmpty(message);
    }

    public Message(String message, Object... args) {
        text = (MutableComponent) Component.nullToEmpty(String.format(message, args));
    }

    public Message(MutableComponent text) {
        this.text = text;
    }

    public Message add(String message) {
        text.append(message);
        return this;
    }

    public Message add(String message, Object... args) {
        text.append(String.format(message, args));
        return this;
    }

    public Message add(Message message) {
        text.append(message.raw());
        return this;
    }

    public Message format(ChatFormatting... format) {
        text.withStyle(format);
        return this;
    }

    public Message fail() {
        text.withStyle(ChatFormatting.RED);
        return this;
    }

    public Message hover(String message) {
        return this.hover(Component.nullToEmpty(message));
    }

    public Message hover(Component message) {
        text.withStyle(s -> s.withHoverEvent(new ShowText(message)));
        return this;
    }

    public Message click(String message) {
        text.withStyle(s -> s.withClickEvent(new RunCommand(message)));
        return this;
    }

    public Message send(Player player, boolean actionBar) {
        player.displayClientMessage(text, actionBar);
        return this;
    }

    public Message send(Faction faction) {
        Message message = this.prependFaction(faction);
        for (User member : faction.getUsers()) {
            ServerPlayer player = manager.getPlayer(member.getID());
            if (player != null) message.send(player, false);
        }
        return this;
    }

    public void sendToGlobalChat() {
        for (ServerPlayer player : manager.getPlayers()) {
            User.ChatMode option = User.get(player.getUUID()).chat;
            if (option != User.ChatMode.FOCUS) player.displayClientMessage(text, false);
        }
    }

    public void sendToFactionChat(Faction faction) {
        for (User member : faction.getUsers()) {
            ServerPlayer player = manager.getPlayer(member.getID());
            player.displayClientMessage(text, false);
        }
    }

    public Message prependFaction(Faction faction) {
        text =
                new Message()
                        .add(
                                new Message(
                                                faction.getColor().toString()
                                                        + ChatFormatting.BOLD
                                                        + faction.getName())
                                        .hover(faction.getDescription()))
                        .filler("»")
                        .raw()
                        .append(text);
        return this;
    }

    public Message filler(String symbol) {
        text.append(
                Component.nullToEmpty(
                        " "
                                + ChatFormatting.RESET
                                + ChatFormatting.DARK_GRAY
                                + symbol
                                + ChatFormatting.RESET
                                + " "));
        return this;
    }

    public MutableComponent raw() {
        return text;
    }
}
