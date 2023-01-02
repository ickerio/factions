package io.icker.factions.util;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.UUID;

public class Message {
    public static PlayerManager manager;
    private MutableText text;
    private final ArrayList<Message> children = new ArrayList<>();

    public Message(String message) {
        text = (MutableText) Text.of(message);
    }

    public Message(Text message) {
        text = (MutableText) message;
    }

    public Message(String message, Object... args) {
        text = (MutableText) Text.of(String.format(message, args));
    }

    public Message add(String message) {
        children.add(new Message(message));
        return this;
    }

    public Message add(String message, Object... args) {
        children.add(new Message(String.format(message, args)));
        return this;
    }

    public Message add(Message message) {
        children.add(message);
        return this;
    }

    public Message format(Formatting... format) {
        text.formatted(format);
        return this;
    }

    public Message fail() {
        text.formatted(Formatting.RED);
        return this;
    }

    public Message hover(String message) {
        text.styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(message))));
        return this;
    }

    public Message click(String message) {
        text.styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message)));
        return this;
    }

    public Message send(PlayerEntity player, boolean actionBar) {
        player.sendMessage(this.build(player.getUuid()), actionBar);
        return this;
    }

    public Message send(Faction faction) {
        Message message = this.prependFaction(faction);
        for (User member : faction.getUsers()) {
            ServerPlayerEntity player = manager.getPlayer(member.getID());
            if (player != null) message.send(player, false);
        }
        return this;
    }

    public void sendToGlobalChat() {
        for (ServerPlayerEntity player : manager.getPlayerList()) {
            User.ChatMode option = User.get(player.getUuid()).chat;
            if (option != User.ChatMode.FOCUS) player.sendMessage(text, false);
        }
    }

    public void sendToFactionChat(Faction faction) {
        for (User member : faction.getUsers()) {
            ServerPlayerEntity player = manager.getPlayer(member.getID());
            player.sendMessage(text, false);
        }
    }

    public Message prependFaction(Faction faction) {
        Message message = new Message("").add(new Message(faction.getColor().toString() + Formatting.BOLD + faction.getName()).hover(faction.getDescription()))
                .filler("»");
        message.add(this);
        return message;
    }

    public Message filler(String symbol) {
        children.add(new Message(Text.of(" " + Formatting.RESET + Formatting.DARK_GRAY + symbol + Formatting.RESET + " ")));
        return this;
    }

    public MutableText raw() {
        MutableText built = text;

        for (Message message : children) {
            built.append(message.raw());
        }

        return built;
    }

    public MutableText build(UUID userId) {
        MutableText built = text;
        if (text.getString().startsWith("translate:")) {
            built = Text.of(Translator.get(text.getString(), User.get(userId).language)).copy().setStyle(text.getStyle());
        }

        for (Message message : children) {
            built.append(message.build(userId));
        }

        return built;
    }
}
