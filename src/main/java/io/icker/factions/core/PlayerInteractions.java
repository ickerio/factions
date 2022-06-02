package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.mixin.BucketItemMixin;
import io.icker.factions.mixin.ItemMixin;
import io.icker.factions.util.Message;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.World;

import java.util.UUID;

public class PlayerInteractions {
    public static boolean preventInteract(ServerPlayerEntity player, World world, BlockHitResult result) {
        BlockPos pos = result.getBlockPos();
        BlockPos placePos = pos.add(result.getSide().getVector());
        return !actionPermitted(pos, world, player) || !actionPermitted(placePos, world, player);
    }

    public static boolean preventUseItem(ServerPlayerEntity player, World world, ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof BucketItem) {
            Fluid fluid = ((BucketItemMixin) item).getFluid();
            FluidHandling handling = fluid == Fluids.EMPTY ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE;

            BlockHitResult result = ItemMixin.invokeRaycast(world, player, handling);

            if (result.getType() != BlockHitResult.Type.MISS) {
                return preventInteract(player, world, result);
            }
        }

        return false;
    }

    public static boolean preventFriendlyFire(ServerPlayerEntity player, ServerPlayerEntity target) {
        return PlayerInteractions.preventFriendlyFire(player, target.getUuid());
    }

    public static boolean preventFriendlyFire(ServerPlayerEntity player, UUID targetID) {
        User source = User.get(player.getUuid());
        User target = User.get(targetID);

        if (!source.isInFaction() && !target.isInFaction()) {
            return false;
        }
        if (!source.isInFaction() || !target.isInFaction()) {
            return true;
        }
        return (source.getFaction().getID() == target.getFaction().getID() || Relationship.get(source.getFaction().getID(), target.getFaction().getID()).mutuallyAllies()) && !FactionsMod.CONFIG.FRIENDLY_FIRE;
    }

    public static void warnPlayer(ServerPlayerEntity target, String action) {
        new Message("Cannot %s in this claim", action)
                .fail()
                .send(target, true);
    }

    public static boolean actionPermitted(BlockPos pos, World world, ServerPlayerEntity player) {
        User member = User.get(player.getUuid());
        if (member.isBypassOn()) {
            if (player.hasPermissionLevel(FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL)) {
                return true;
            } else {
                member.setBypass(false);
            }
        }

        String dimension = world.getRegistryKey().getValue().toString();
        ChunkPos actionPos = world.getChunk(pos).getPos();

        Claim claim = Claim.get(actionPos.x, actionPos.z, dimension);
        if (claim == null) return true;

        if (!member.isInFaction()) {
            syncBlocks(player, world, pos);
            return false;
        }

        Faction claimOwner = claim.getFaction();
        Faction memberFaction = member.getFaction();

        boolean overclaimed = claimOwner.getClaims().size() * FactionsMod.CONFIG.CLAIM_WEIGHT > claimOwner.getPower();
        boolean validMember = claimOwner.getID() == memberFaction.getID();
        boolean allied = Relationship.get(claimOwner.getID(), memberFaction.getID()).mutuallyAllies();

        boolean permitted = overclaimed || validMember || allied;
        if (!permitted) syncBlocks(player, world, pos);
        return permitted;
    }

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
        if (FactionsMod.CONFIG.RADAR && User.get(player.getUuid()).isRadarOn()) {
            ServerWorld world = player.getWorld();
            String dimension = world.getRegistryKey().getValue().toString();

            ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();

            Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);

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
