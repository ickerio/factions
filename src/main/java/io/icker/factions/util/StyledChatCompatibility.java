package io.icker.factions.util;

import eu.pb4.styledchat.other.StyledChatSentMessage;

import net.minecraft.network.message.SentMessage;

import java.util.UUID;

public class StyledChatCompatibility {
    public static UUID getSender(SentMessage message) {
        return ((StyledChatSentMessage.Chat) message).message().link().sender();
    }

    public static boolean isNotPlayer(SentMessage message) {
        return !(message instanceof StyledChatSentMessage.Chat);
    }
}
