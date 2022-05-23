package io.icker.factions.event;

import io.icker.factions.FactionsMod;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.database.PlayerConfig;
import io.icker.factions.database.PlayerConfig.ChatOption;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class ChatEvents {
    public static Text handleMessage(ServerPlayerEntity sender, String message) {
        UUID id = sender.getUuid();
        Member member = Member.get(id);

        if (PlayerConfig.get(id).chat == ChatOption.GLOBAL) {
            if (member == null) {
                return ChatEvents.global(sender, message);
            } else {
                return ChatEvents.memberGlobal(sender, member.getFaction(), message);
            }
        } else {
            if (member == null) {
                return ChatEvents.global(sender, message);
            } else {
                return ChatEvents.faction(sender, member.getFaction(), message);
            }
        }
    }

    public static Text global(ServerPlayerEntity sender, String message) {
        return new Message(message).format(Formatting.GRAY)
                .raw();
    }

    public static Text memberGlobal(ServerPlayerEntity sender, Faction faction, String message) {
        return new Message("")
                .add(new Message(faction.name).format(Formatting.BOLD, faction.color))
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .raw();
    }

    public static Text faction(ServerPlayerEntity sender, Faction faction, String message) {
        return new Message("")
                .add(new Message("F").format(Formatting.BOLD, faction.color))
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .raw();
    }
}
