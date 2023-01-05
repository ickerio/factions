package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.text.Message;
import io.icker.factions.text.PlainText;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

public class MapCommand implements Command{
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getWorld();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();
        String dimension = world.getRegistryKey().getValue().toString();

        Message message = new Message();

        // Print the header of the faction map.
        message.append(new TranslatableText("translate:map.title"));

        for (int z = -4; z <= 5; z++) { // Rows (10)
            for (int x = -5; x <= 5; x++) { // Columns (11)
                Claim claim = Claim.get(chunkPos.x + x, chunkPos.z + z, dimension);
                if (x == 0 && z == 0) { // Check if middle (your chunk)
                    if (claim == null) {
                        message.append(new TranslatableText("⏺").hover("<You> <Wilderness>").format(Formatting.DARK_GRAY));
                    } else {
                        Faction owner = claim.getFaction();
                        message.append(new TranslatableText("⏺").hover("<You> " + owner.getName()).format(owner.getColor()));
                    }
                } else {
                    if (claim == null) {
                        message.append(new PlainText("□").format(Formatting.DARK_GRAY));
                    } else {
                        Faction owner = claim.getFaction();
                        message.append(new PlainText("■").hover(owner.getName()).format(owner.getColor()));
                    }
                }
                message.append(new PlainText(" "));
            }
            message.append(new PlainText("\n"));
        }

        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("map")
            .requires(Requires.hasPerms("factions.map", 0))
            .executes(this::run)
            .build();
    }
}
