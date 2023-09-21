package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

public class MapCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = (ServerWorld) player.getWorld();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        String dimension = world.getRegistryKey().getValue().toString();

        // Print the header of the faction map.
        new Message(Formatting.DARK_GRAY + "──┤" + Formatting.GREEN + " Faction Map"
                + Formatting.DARK_GRAY + "├──").send(player, false);

        for (int z = -4; z <= 5; z++) { // Rows (10)
            Message row = new Message("");
            for (int x = -5; x <= 5; x++) { // Columns (11)
                Claim claim = Claim.get(chunkPos.x + x, chunkPos.z + z, dimension);
                if (x == 0 && z == 0) { // Check if middle (your chunk)
                    if (claim == null) {
                        row.add(new Message("⏺").format(Formatting.DARK_GRAY)
                                .hover("<You> <Wilderness>"));
                    } else {
                        Faction owner = claim.getFaction();
                        row.add(new Message("⏺").format(owner.getColor())
                                .hover("<You> " + owner.getName()));
                    }
                } else {
                    if (claim == null) {
                        row.add("□").format(Formatting.DARK_GRAY);
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
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("map").requires(Requires.hasPerms("factions.map", 0))
                .executes(this::run).build();
    }
}
