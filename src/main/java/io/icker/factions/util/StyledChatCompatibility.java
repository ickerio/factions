package io.icker.factions.util;

import eu.pb4.styledchat.other.StyledChatSentMessage;

import net.minecraft.network.chat.OutgoingChatMessage;

import java.util.UUID;

public class StyledChatCompatibility {
    public static UUID getSender(OutgoingChatMessage message) {
        return ((StyledChatSentMessage.Chat) message).message().link().sender();
    }

    public static boolean isNotPlayer(OutgoingChatMessage message) {
        return !(message instanceof StyledChatSentMessage.Chat);
    }
}
