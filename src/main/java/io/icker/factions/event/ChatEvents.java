package io.icker.factions.event;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.User.ChatMode;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class ChatEvents {
    public static Text handleMessage(ServerPlayerEntity sender, String message) {
        UUID id = sender.getUuid();
        User member = User.get(id);

        if (member.getChatMode() == ChatMode.GLOBAL) {
            if (member.isInFaction()) {
                return ChatEvents.inFactionGlobal(sender, member.getFaction(), message);
            } else {
                return ChatEvents.global(sender, message);
            }
        } else {
            if (member.isInFaction()) {
                return ChatEvents.faction(sender, member.getFaction(), message);
            } else {
                return ChatEvents.global(sender, message);
            }
        }
    }

    public static Text global(ServerPlayerEntity sender, String message) {
        return new Message(message).format(Formatting.GRAY)
                .raw();
    }

    public static Text inFactionGlobal(ServerPlayerEntity sender, Faction faction, String message) {
        return new Message("")
                .add(new Message(faction.getName()).format(Formatting.BOLD, faction.getColor()))
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .raw();
    }

    public static Text faction(ServerPlayerEntity sender, Faction faction, String message) {
        return new Message("")
                .add(new Message("F").format(Formatting.BOLD, faction.getColor()))
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .raw();
    }
}
