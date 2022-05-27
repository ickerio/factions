package io.icker.factions.event;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.User.ChatMode;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class ChatEvents {
    public static void handleMessage(ServerPlayerEntity sender, String message) {
        UUID id = sender.getUuid();
        User member = User.get(id);

        if (member.getChatMode() == ChatMode.GLOBAL) {
            if (member.isInFaction()) {
                ChatEvents.inFactionGlobal(sender, member.getFaction(), message);
            } else {
                ChatEvents.global(sender, message);
            }
        } else {
            if (member.isInFaction()) {
                ChatEvents.faction(sender, member.getFaction(), message);
            } else {
                ChatEvents.fail(sender);
            }
        }
    }

    

    public static void global(ServerPlayerEntity sender, String message) {
        FactionsMod.LOGGER.info("[" + sender.getName().asString() + " -> All] " + message);
        new Message(sender.getName().asString())
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .sendToGlobalChat();
    }

    public static void inFactionGlobal(ServerPlayerEntity sender, Faction faction, String message) {
        FactionsMod.LOGGER.info("[" + faction.getName() + " " + sender.getName().asString() + " -> All] " + message);
        String rank = "";
        for (User member : faction.getMembers())
            if (member.getID().equals(sender.getUuid()))
                rank = member.getRank().name().toLowerCase().replace("_", " ");

        new Message("")
                .add(new Message(faction.getName()).format(Formatting.BOLD, faction.getColor()))
                .add(" " + rank)
                .add(" " + sender.getName().asString())
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .sendToGlobalChat();
    }

    public static void fail(ServerPlayerEntity sender) {
        new Message("You must be in a faction to use faction chat")
                .hover("Click to join global chat")
                .click("/f chat global")
                .fail()
                .send(sender, false);
    }

    public static void faction(ServerPlayerEntity sender, Faction faction, String message) {
        FactionsMod.LOGGER.info("[" + faction.getName() + " " + sender.getName().asString() + " -> " + faction.getName() + "] " + message);
        new Message(sender.getName().asString())
                .add(new Message(" F").format(Formatting.BOLD, faction.getColor()))
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .sendToFactionChat(faction);
    }
}
