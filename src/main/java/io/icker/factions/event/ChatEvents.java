package io.icker.factions.event;

import java.util.UUID;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.database.PlayerConfig;
import io.icker.factions.database.PlayerConfig.ChatOption;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class ChatEvents {
    public static void handleMessage(ServerPlayerEntity sender, String message) {
        UUID id = sender.getUuid();
        Member member = Member.get(id);

        if (PlayerConfig.get(id).chat == ChatOption.GLOBAL) {
            if (member == null) {
                ChatEvents.global(sender, message);
            } else {
                ChatEvents.memberGlobal(sender, member.getFaction(), message);
            }
        } else {
            if (member == null) {
                ChatEvents.fail(sender);
            } else {
                ChatEvents.faction(sender, member.getFaction(), message);
            }
        }
    }

    public static void global(ServerPlayerEntity sender, String message) {
        new Message(sender.getName().asString())
            .filler("»")
            .add(new Message(message).format(Formatting.GRAY))
            .sendToGlobalChat();
    }

    public static void memberGlobal(ServerPlayerEntity sender, Faction faction, String message) {
        new Message("")
            .add(new Message(faction.name).format(Formatting.BOLD, faction.color))
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
        new Message(sender.getName().asString())
            .add(new Message(" F").format(Formatting.BOLD, faction.color))
            .filler("»")
            .add(new Message(message).format(Formatting.GRAY))
            .sendToFactionChat(faction);
    }
}
