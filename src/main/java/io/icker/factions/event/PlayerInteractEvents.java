package io.icker.factions.event;

import io.icker.factions.config.Config;
import io.icker.factions.database.Claim;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import io.icker.factions.mixin.BucketItemAccessor;
import io.icker.factions.mixin.ItemInvoker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.KnowledgeBookItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.Wearable;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FluidModificationItem;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;

public class PlayerInteractEvents {
    public static boolean preventInteract(ServerPlayerEntity player, World world, BlockPos pos) {
        return !actionPermitted(pos, world, player);
    }
    
    public static boolean preventUseItem(ServerPlayerEntity player, World world, ItemStack stack) {
        if (stack.getUseAction() != UseAction.NONE) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof Wearable             ||
            item instanceof SnowballItem         ||
            item instanceof EggItem              ||
            item instanceof FishingRodItem       ||
            item instanceof BundleItem           ||
            item instanceof EnderEyeItem         ||
            item instanceof ExperienceBottleItem ||
            item instanceof KnowledgeBookItem    ||
            item instanceof EnderPearlItem       ){
            return false;
        }
        BlockHitResult blockHitResult;
        if (item instanceof BucketItem) {
            FluidHandling fluidHandling = ((BucketItemAccessor)item).getFluid() == Fluids.EMPTY ? 
                    RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE;
            blockHitResult = ItemInvoker.invokeRaycast(world, player, fluidHandling);
        } else {
            blockHitResult = ItemInvoker.invokeRaycast(world, player, RaycastContext.FluidHandling.NONE);
        }
        if (item instanceof FluidModificationItem || item instanceof BlockItem) {
            resyncBlock(player, world, blockHitResult.getBlockPos());
            return true;
        }
        if (blockHitResult.getType() != BlockHitResult.Type.MISS) {
            return !actionPermitted(blockHitResult.getBlockPos(), world, player);
        }
        return false;
    }

    public static boolean preventFriendlyFire(ServerPlayerEntity player, ServerPlayerEntity target) {
        Member playerMember = Member.get(player.getUuid());
        Member targetMember = Member.get(target.getUuid());

        if (playerMember == null || targetMember == null) return false;
        return playerMember.getFaction().name == targetMember.getFaction().name;
    }

    public static void warnPlayer(PlayerEntity target, String action) {
        new Message("Unable to %s in this claim", action)
            .format(Formatting.RED, Formatting.BOLD)
            .send(target, true);
    }

    static boolean actionPermitted(BlockPos pos, World world, ServerPlayerEntity player) {
        String dimension = world.getRegistryKey().getValue().toString();
        ChunkPos actionPos = world.getChunk(pos).getPos();

        Claim claim = Claim.get(actionPos.x, actionPos.z, dimension);
        if (claim == null) return true;

        Member member = Member.get(player.getUuid());
        Faction owner = claim.getFaction();

        boolean overclaimed = owner.getClaims().size() * Config.CLAIM_WEIGHT > owner.power;
        boolean validMember = member == null ? false : member.getFaction().name == owner.name;

        if (overclaimed || validMember == false) {
            resyncBlock(player, world, pos);
            return false;
        }
        return true;
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

    public static void resyncBlock(ServerPlayerEntity player, World world, BlockPos pos) {
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, pos.add(x, y, z)));
                }
            }
        }
    }
}