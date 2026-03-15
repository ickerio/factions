package io.icker.factions.util;

import eu.pb4.styledchat.other.StyledChatSentMessage;
import java.util.UUID;
import net.minecraft.network.chat.OutgoingChatMessage;

public class StyledChatCompatibility {
    public static UUID getSender(OutgoingChatMessage message) {
        return ((StyledChatSentMessage.Chat) message).message().link().sender();
    }

    public static boolean isNotPlayer(OutgoingChatMessage message) {
        return !(message instanceof StyledChatSentMessage.Chat);
    }
}
