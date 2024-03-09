package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.core.FactionsManager;
import io.icker.factions.util.Command;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;

public class BurnResourcesCommand implements Command {
    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("burn-resources").requires(Requires.isMember()).executes(this::run).build();
    }

    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ItemStack itemStack = player.getMainHandStack();
        boolean isDiamond = itemStack.isOf(Items.DIAMOND);
        if(!isDiamond) {
            player.sendMessage(new LiteralText("§cYou don't have any diamonds in main hand!"), MessageType.CHAT, Util.NIL_UUID);
            return 0;
        }
        User user = User.get(player.getName().getString());
        Faction faction = user.getFaction();
        int wallet = itemStack.getCount()*FactionsMod.CONFIG.DIAMOND_CURRENCY;
        itemStack.setCount(0);
        faction.adjustPower(wallet);
        player.sendMessage(new LiteralText("§aAdded "+wallet+" power to the " + faction.getName() + " Faction"), MessageType.CHAT, Util.NIL_UUID);
        return 1;
    }
}
