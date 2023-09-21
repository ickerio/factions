package io.icker.factions.util;

import java.util.UUID;
import eu.pb4.styledchat.other.StyledChatSentMessage;
import net.minecraft.network.message.SentMessage;

public class StyledChatCompatibility {
    public static UUID getSender(SentMessage message) {
        return ((StyledChatSentMessage.Chat) message).message().link().sender();
    }

    public static boolean isNotPlayer(SentMessage message) {
        return !(message instanceof StyledChatSentMessage.Chat);
    }
}
