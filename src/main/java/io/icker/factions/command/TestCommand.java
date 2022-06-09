package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.block.GravelBlock;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;

public class TestCommand implements Command {

    private int run(CommandContext<ServerCommandSource> context) {
        ServerPlayNetworkHandler networkHandler = context.getSource().getPlayer().networkHandler;
        Faction faction = User.get(context.getSource().getPlayer().getUuid()).getFaction();

        networkHandler.sendPacket(new OpenScreenS2CPacket(faction.syncId, ScreenHandlerType.GENERIC_9X3, Text.of("test")));

        DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        EnderChestInventory safe = faction.getSafe();

        for (int i = 0; i < 27; i++) {
            items.set(i, safe.getStack(i));
        }

        networkHandler.sendPacket(new InventoryS2CPacket(faction.syncId, 1, items, ItemStack.EMPTY));
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
                .literal("test")
                .executes(this::run)
                .build();
    }
}
