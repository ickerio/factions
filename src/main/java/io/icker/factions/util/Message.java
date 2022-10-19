package io.icker.factions.util;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.War;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Message {
    public static PlayerManager manager;
    private MutableText text;

    public Message(String message) {
        text = (MutableText) Text.of(message);
    }

    public Message(String message, Object... args) {
        text = (MutableText) Text.of(String.format(message, args));
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
        player.sendMessage(text, actionBar);
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

    public Message send(War war) {
        Message message = this.prependMessage(new Message(war.getName()).format(Formatting.RED));
        for (Faction faction : war.getFactions()) {
            message.sendToFactionChat(faction);
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
        return prependMessage(new Message(faction.getColor().toString() + Formatting.BOLD + faction.getName()).hover(faction.getDescription()));
    }

    public Message prependMessage(Message message) {
        text = new Message("")
                .add(message)
                .filler("»")
                .raw()
                .append(text);
        return this;
    }

    public Message filler(String symbol) {
        text.append(Text.of(" " + Formatting.RESET + Formatting.DARK_GRAY + symbol + Formatting.RESET + " "));
        return this;
    }

    public MutableText raw() {
        return text;
    }
}
