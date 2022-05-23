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
                return ChatEvents.fail(sender);
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
        String rank = "";
        for (Member member : faction.getMembers())
            if (member.uuid.equals(sender.getUuid()))
                rank = member.getRank().name().toLowerCase().replace("_", " ");

        return new Message("")
                .add(new Message(faction.name).format(Formatting.BOLD, faction.color))
                .filler("»")
                .add(new Message(message).format(Formatting.GRAY))
                .raw();
    }

    public static Text fail(ServerPlayerEntity sender) {
        return new Message("You must be in a faction to use faction chat")
                .hover("Click to join global chat")
                .click("/f chat global")
                .fail()
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
