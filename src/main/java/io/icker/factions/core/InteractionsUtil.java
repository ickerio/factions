package io.icker.factions.core;

import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class InteractionsUtil {
    public static void sync(PlayerEntity player, ItemStack itemStack, Hand hand) {
        player.setStackInHand(hand, itemStack);
        itemStack.setCount(itemStack.getCount());
        if (itemStack.isDamageable()) {
            itemStack.setDamage(itemStack.getDamage());
        }

        if (!player.isUsingItem()) {
            player.playerScreenHandler.syncState();
        }
    }

    public static void warn(PlayerEntity player, InteractionsUtilActions action) {
        SoundManager.warningSound(player);
        User user = User.get(player.getUuid());
        new Message(
                        Text.translatable(
                                "factions.interactions.cannot_do",
                                Text.translatable(
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
