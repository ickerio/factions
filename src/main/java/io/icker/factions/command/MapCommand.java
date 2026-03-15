package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class MapCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = (ServerLevel) player.level();

        ChunkPos chunkPos = world.getChunk(player.blockPosition()).getPos();
        String dimension = world.dimension().identifier().toString();

        // Print the header of the faction map.
        new Message(
                        Component.literal("──┤ ")
                                .withStyle(ChatFormatting.DARK_GRAY)
                                .append(
                                        Component.translatable("factions.command.map.title")
                                                .withStyle(ChatFormatting.GREEN))
                                .append(
                                        Component.literal("├──")
                                                .withStyle(ChatFormatting.DARK_GRAY)))
                .send(player, false);

        for (int z = -4; z <= 4; z++) { // Rows (9)
            Message row = new Message();
            for (int x = -5; x <= 5; x++) { // Columns (11)
                Claim claim = Claim.get(chunkPos.x + x, chunkPos.z + z, dimension);
                if (x == 0 && z == 0) { // Check if middle (your chunk)
                    if (claim == null) {
                        row.add(
                                new Message("⏺")
                                        .format(ChatFormatting.DARK_GRAY)
                                        .hover(
                                                Component.translatable(
                                                        "factions.command.map.you_wilderness")));
                    } else {
                        Faction owner = claim.getFaction();
                        row.add(
                                new Message("⏺")
                                        .format(owner.getColor())
                                        .hover(
                                                Component.translatable(
                                                        "factions.command.map.you_owner",
                                                        owner.getName())));
                    }
                } else {
                    if (claim == null) {
                        row.add("□").format(ChatFormatting.DARK_GRAY);
                    } else {
                        Faction owner = claim.getFaction();
                        row.add(new Message("■").format(owner.getColor()).hover(owner.getName()));
                    }
                }
                row.add(" ");
            }
            row.send(player, false);
        }

        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("map")
                .requires(Requires.hasPerms("factions.map", 0))
                .executes(this::run)
                .build();
    }
}
