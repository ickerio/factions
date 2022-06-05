package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;


public class PlayerInteractions {
    public static void syncItem(ServerPlayerEntity player, ItemStack itemStack, Hand hand) {
        player.setStackInHand(hand, itemStack);
        itemStack.setCount(itemStack.getCount());
        if (itemStack.isDamageable()) {
            itemStack.setDamage(itemStack.getDamage());
        }

        if (!player.isUsingItem()) {
            player.playerScreenHandler.syncState();
        }
    }

    public static void syncBlocks(ServerPlayerEntity player, World world, BlockPos pos) {
        for (int x = -1; x < 2; x++) { // TODO: this is slighty inefficent as it may do some blocks twice
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, pos.add(x, y, z)));
                }
            }
        }
    }

    public static void onMove(ServerPlayerEntity player) {
        User user = User.get(player.getUuid());
        ServerWorld world = player.getWorld();
        String dimension = world.getRegistryKey().getValue().toString();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();

        Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);
        if (user.getAutoclaim() && claim == null) {
            Faction faction = user.getFaction();
            int requiredPower = (faction.getClaims().size() + 1) * FactionsMod.CONFIG.CLAIM_WEIGHT;
            int maxPower = faction.getUsers().size() * FactionsMod.CONFIG.MEMBER_POWER + FactionsMod.CONFIG.BASE_POWER;

            if (maxPower < requiredPower) {
                new Message("Not enough faction power to claim chunk, autoclaim toggled off").fail().send(player, false);
                user.toggleAutoclaim();
            } else {
                faction.addClaim(chunkPos.x, chunkPos.z, dimension);
                claim = Claim.get(chunkPos.x, chunkPos.z, dimension);
                new Message("Chunk (%d, %d) claimed by %s", chunkPos.x, chunkPos.z, player.getName().asString())
                    .send(faction);
            }
        }
        if (FactionsMod.CONFIG.RADAR && user.isRadarOn()) {
            if (claim != null) {
                new Message(claim.getFaction().getName())
                        .format(claim.getFaction().getColor())
                        .send(player, true);
            } else {
                new Message("Wilderness")
                        .format(Formatting.GREEN)
                        .send(player, true);
            }
        }
    }
}
