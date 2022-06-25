package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class ChatManager {
    public static void handleMessage(ServerPlayerEntity sender, String message) {
        UUID id = sender.getUuid();
        User member = User.get(id);

        if (member.chat == User.ChatMode.GLOBAL) {
            if (member.isInFaction()) {
                ChatManager.inFactionGlobal(sender, member.getFaction(), message);
            } else {
                ChatManager.global(sender, message);
            }
        } else {
            if (member.isInFaction()) {
                ChatManager.faction(sender, member.getFaction(), message);
            } else {
                ChatManager.fail(sender);
            }
        }
    }

    private static void global(ServerPlayerEntity sender, String message) {
        FactionsMod.LOGGER.info("[" + sender.getName().asString() + " -> All] " + message);
        new Message(String.format("[%s] ") + message).format(Formatting.GRAY).sendToGlobalChat();
    }

    private static void inFactionGlobal(ServerPlayerEntity sender, Faction faction, String message) {
        FactionsMod.LOGGER.info("[" + faction.getName() + " " + sender.getName().asString() + " -> All] " + message);
        new Message(String.format("[%s] "))
            .add(new Message(faction.getName()).format(Formatting.BOLD, faction.getColor()))
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

    private static void faction(ServerPlayerEntity sender, Faction faction, String message) {
        FactionsMod.LOGGER.info("[" + faction.getName() + " " + sender.getName().asString() + " -> " + faction.getName() + "] " + message);
        new Message(String.format("[%s] "))
            .add(new Message("F").format(Formatting.BOLD, faction.getColor()))
            .filler("»")
            .add(new Message(message).format(Formatting.GRAY))
            .sendToFactionChat(faction);
    }
}
