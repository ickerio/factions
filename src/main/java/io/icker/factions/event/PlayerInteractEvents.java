package io.icker.factions.event;

import io.icker.factions.config.Config;
import io.icker.factions.database.Claim;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext;
import net.minecraft.item.Item;
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
        if (item instanceof FluidModificationItem || item instanceof BlockItem) {
            return true;
        } else if (item instanceof Wearable             ||
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
        BlockHitResult blockHitResult = raycast(world, player, RaycastContext.FluidHandling.NONE);
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
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    for (int z = -1; z < 2; z++) {
                        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, pos.add(x, y, z)));
                    }
                }
            }
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

    private static BlockHitResult raycast(World world, PlayerEntity player, RaycastContext.FluidHandling fluidHandling) {
        float f = player.getPitch();
        float g = player.getYaw();
        Vec3d vec3d = player.getEyePos();
        float h = MathHelper.cos(-g * 0.017453292F - 3.1415927F);
        float i = MathHelper.sin(-g * 0.017453292F - 3.1415927F);
        float j = -MathHelper.cos(-f * 0.017453292F);
        float k = MathHelper.sin(-f * 0.017453292F);
        float l = i * j;
        float n = h * j;
        Vec3d vec3d2 = vec3d.add((double)l * 5.0D, (double)k * 5.0D, (double)n * 5.0D);
        return world.raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.OUTLINE, fluidHandling, player));
     }
}