package io.icker.factions.util;

import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class FactionsUtil {
    public class Message {
        public static void sendSuccess(ServerPlayerEntity player, String message) {
            sendWithColour(player, message, Formatting.GREEN);
        }
    
        public static void sendError(ServerPlayerEntity player, String message) {
            sendWithColour(player, message, Formatting.RED);
        }
    
        public static void sendWithColour(ServerPlayerEntity player, String message, Formatting colour) {
            player.sendMessage(new LiteralText(message).setStyle(Style.EMPTY.withFormatting(colour)), MessageType.CHAT, net.minecraft.util.Util.NIL_UUID);
        }
    }
}
