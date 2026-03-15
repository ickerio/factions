package io.icker.factions.core;

import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class InteractionsUtil {
    public static void sync(ServerPlayer player, ItemStack itemStack, InteractionHand hand) {
        player.setItemInHand(hand, itemStack);
        itemStack.setCount(itemStack.getCount());
        if (itemStack.isDamageableItem()) {
            itemStack.setDamageValue(itemStack.getDamageValue());
        }

        if (!player.isUsingItem()) {
            player.inventoryMenu.sendAllDataToRemote();
        }
    }

    public static void warn(ServerPlayer player, InteractionsUtilActions action) {
        SoundManager.warningSound(player);
        User user = User.get(player.getUUID());
        new Message(
                        Component.translatable(
                                "factions.interactions.cannot_do",
                                Component.translatable(
                                        "factions.interactions.name."
                                                + action.toString().toLowerCase())))
                .fail()
                .send(player, !user.radar);
    }

    public enum InteractionsUtilActions {
        BREAK_BLOCKS,
        USE_BLOCKS,
        PLACE_BLOCKS,
        PLACE_OR_PICKUP_LIQUIDS,
        ATTACK_ENTITIES,
        USE_ENTITIES,
        USE_INVENTORY
    }
}
