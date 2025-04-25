package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;

import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class ChatManager {
    public static void register() {
        ServerMessageDecoratorEvent.EVENT.register(
                ServerMessageDecoratorEvent.CONTENT_PHASE,
                (sender, message) -> {
                    if (sender != null && FactionsMod.CONFIG.DISPLAY.MODIFY_CHAT) {
                        return ChatManager.handleMessage(sender, message.getString());
                    }
                    return message;
                });
    }

    public static Text handleMessage(ServerPlayerEntity sender, String message) {
        UUID id = sender.getUuid();
        User member = User.get(id);

        if (member.chat == User.ChatMode.GLOBAL) {
            if (member.isInFaction()) {
                return ChatManager.inFactionGlobal(sender, member.getFaction(), message);
            } else {
                return ChatManager.global(sender, message);
            }
        } else {
            if (member.isInFaction()) {
                return ChatManager.faction(sender, member.getFaction(), message);
            } else {
                return ChatManager.global(sender, message);
            }
        }
    }

    private static Text global(ServerPlayerEntity sender, String message) {
        return new Message(message).format(Formatting.GRAY).raw();
    }

    private static Text inFactionGlobal(
            ServerPlayerEntity sender, Faction faction, String message) {
        return new Message()
                .add(new Message(faction.getName()).format(Formatting.BOLD, faction.getColor()))
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .raw();
    }

    private static Text faction(ServerPlayerEntity sender, Faction faction, String message) {
        return new Message()
                .add(
                        new Message(Text.translatable("factions.chat.in_faction_symbol"))
                                .format(Formatting.BOLD, faction.getColor()))
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .raw();
    }
}
