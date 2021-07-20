package io.icker.factions.util;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.database.PlayerConfig;
import io.icker.factions.database.PlayerConfig.ChatOption;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

public class Message {
    public static PlayerManager manager;
    private MutableText text;

    public Message(String message) {
        text = new LiteralText(message);
    }

    public Message(String message, Object... args) {
        text = new LiteralText(String.format(message, args));
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
        text.styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(message))));
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
        for (Member member : faction.getMembers()) {
            ServerPlayerEntity player = manager.getPlayer(member.uuid);
            if (player != null) message.send(player, false);
        }
        return this;
    }

    public void sendToGlobalChat() {
        for (ServerPlayerEntity player : manager.getPlayerList()) {
            ChatOption option = PlayerConfig.get(player.getUuid()).chat;
            if (option != ChatOption.FOCUS) player.sendMessage(text, false);
        }
    }

    public void sendToFactionChat(Faction faction) {
        for (Member member : faction.getMembers()) {
            ServerPlayerEntity player = manager.getPlayer(member.uuid);
            player.sendMessage(text, false);
        }
    }

    public Message prependFaction(Faction faction) {
        text = new Message("")
            .add(new Message(faction.color.toString() + Formatting.BOLD + faction.name).hover(faction.description))
            .filler("»")
            .raw()
            .append(text);
        return this;
    }

    public Message filler(String symbol) {
        text.append(new LiteralText(" " + Formatting.RESET + Formatting.DARK_GRAY + symbol +  Formatting.RESET +  " "));
        return this;
    }

    public MutableText raw() {
        return text;
    }
}
