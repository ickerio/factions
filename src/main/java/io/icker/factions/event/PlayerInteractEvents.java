package io.icker.factions.event;

import io.icker.factions.FactionsMod;
import io.icker.factions.config.Config;
import io.icker.factions.database.Claim;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.database.Ally;
import io.icker.factions.database.PlayerConfig;
import io.icker.factions.util.Message;
import io.icker.factions.mixin.BucketItemMixin;
import io.icker.factions.mixin.ItemMixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.UUID;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import java.util.UUID;

public class PlayerInteractEvents {
    public static boolean preventInteract(ServerPlayerEntity player, World world, BlockHitResult result) {
        BlockPos pos = result.getBlockPos();
        BlockPos placePos = pos.add(result.getSide().getVector());
        return !actionPermitted(pos, world, player) || !actionPermitted(placePos, world, player);
    }
    
    public static boolean preventUseItem(ServerPlayerEntity player, World world, ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof BucketItem) {
            Fluid fluid = ((BucketItemMixin)item).getFluid();
            FluidHandling handling = fluid == Fluids.EMPTY ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE;

            BlockHitResult result = ItemMixin.invokeRaycast(world, player, handling);

            if (result.getType() != BlockHitResult.Type.MISS) {
                return preventInteract(player, world, result);
            }
        }

        return false;
    }

    public static boolean preventFriendlyFire(ServerPlayerEntity player, ServerPlayerEntity target) {
        return PlayerInteractEvents.preventFriendlyFire(player, target.getUuid());
    }

    public static boolean preventFriendlyFire(ServerPlayerEntity player, UUID target) {
        Member playerMember = Member.get(player.getUuid());
        Member targetMember = Member.get(target);

        if (playerMember == null || targetMember == null) return false;
        return ((playerMember.getFaction().name == targetMember.getFaction().name  || Ally.checkIfAlly(playerMember.getFaction().name, targetMember.getFaction().name))) && !Config.FRIENDLY_FIRE;
    }

    public static void warnPlayer(ServerPlayerEntity target, String action) {
        new Message("Cannot %s in this claim", action)
            .fail()
            .send(target, true);
    }

    public static boolean actionPermitted(BlockPos pos, World world, ServerPlayerEntity player) {
        PlayerConfig config = PlayerConfig.get(player.getUuid());
        if (config.bypass) {
            if (player.hasPermissionLevel(Config.REQUIRED_BYPASS_LEVEL)) {
                return true;
            } else {
                config.setBypass(false);
            }
        }

        String dimension = world.getRegistryKey().getValue().toString();
        ChunkPos actionPos = world.getChunk(pos).getPos();

        Claim claim = Claim.get(actionPos.x, actionPos.z, dimension);
        if (claim == null) return true;

        Member member = Member.get(player.getUuid());
        Faction owner = claim.getFaction();

        if (member != null) {
            String factionName = member.getFaction().name;
            boolean overclaimed = owner.getClaims().size() * Config.CLAIM_WEIGHT > owner.power;
            boolean validMember = factionName == owner.name;
            boolean allied = Ally.checkIfAlly(owner.name, factionName);

            boolean permitted = overclaimed || validMember || allied;
            if (!permitted) syncBlocks(player, world, pos);
            return permitted;
        } else {
            syncBlocks(player, world, pos);
            return false;
        }
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
        if (PlayerConfig.get(player.getUuid()).currentZoneMessage && Config.ZONE_MESSAGE) {
            ServerWorld world = player.getWorld();
            String dimension = world.getRegistryKey().getValue().toString();

            ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();

            Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);

            if (claim != null) {
                new Message(claim.getFaction().name)
                        .format(claim.getFaction().color)
                        .send(player, true);
            } else {
                new Message("Wilderness")
                        .format(Formatting.GREEN)
                        .send(player, true);
            }
        }
    }
}