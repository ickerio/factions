package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.api.persistents.User;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class ClaimManager {
    public static void register() {
        PlayerEvents.PLACE_BLOCK.register(ClaimManager::onAction);
        PlayerBlockBreakEvents.BEFORE.register(ClaimManager::onBreakBlock);
        UseItemCallback.EVENT.register(ClaimManager::onUseItem);
        PlayerEvents.USE_BUCKET.register(ClaimManager::onUseBucket);
        AttackEntityCallback.EVENT.register(ClaimManager::onAttackEntity);
        PlayerEvents.IS_INVULNERABLE.register(ClaimManager::isInvulnerableTo);
    }

    private static ActionResult onAction(PlayerEntity player, BlockPos position, World world) {
        User user = User.get(player.getUuid());
        if (player.hasPermissionLevel(FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL) && user.isBypassOn()) {
            System.out.println("Passing cos bypass");
            return ActionResult.PASS;
        }

        String dimension = world.getRegistryKey().getValue().toString();
        ChunkPos chunkPosition = world.getChunk(position).getPos();

        Claim claim = Claim.get(chunkPosition.x, chunkPosition.z, dimension); // notes: bypass manager that returns ActionResult.SUCCESS;
        if (claim == null) return ActionResult.PASS;

        if (!user.isInFaction()) {
            //syncBlocks(player, world, pos);
            System.out.println("Failing cos not in faction");
            return ActionResult.FAIL;
        }

        Faction claimFaction = claim.getFaction();
        Faction userFaction = user.getFaction();

        if (claimFaction == userFaction) {
            System.out.println("Passing cos my faction");
            return ActionResult.PASS;
        }

        if (Relationship.get(claimFaction.getID(), userFaction.getID()).mutuallyAllies()) {
            System.out.println("Passing cos allies");
            return ActionResult.PASS;
        }

        if (claimFaction.getClaims().size() * FactionsMod.CONFIG.CLAIM_WEIGHT > claimFaction.getPower()) {
            // syncBlocks(player, world, pos);
            System.out.println("Failing cos no power");
            return ActionResult.FAIL;
        }

        System.out.println("Passing at end");
        return ActionResult.FAIL;
    }

    private static boolean onBreakBlock(World world, PlayerEntity player, BlockPos pos, BlockState state,  BlockEntity blockEntity) {
        ActionResult result = onAction(player, pos, world);
        return result != ActionResult.FAIL;
    }

    private static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, World world, Hand hand) {
        ActionResult result = onAction(player, player.getBlockPos(), world);
        return new TypedActionResult<ItemStack>(result, player.getStackInHand(hand));
        // Maybe syncItem
    }

    private static boolean onUseBucket(PlayerEntity player, World world, BlockPos pos, BlockHitResult hitResult) {
        ActionResult placementResult = onAction(player, pos, world);
        ActionResult playerResult = onAction(player, player.getBlockPos(), world);
        return (placementResult != ActionResult.FAIL) && (playerResult != ActionResult.FAIL);
    }

    private static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (entity.isPlayer()) {
            return isInvulnerableTo(player, entity) ? ActionResult.FAIL : ActionResult.PASS;
        }

        if (!entity.isLiving()) {
            ActionResult entityResult = onAction(player, entity.getBlockPos(), world);
            ActionResult playerResult = onAction(player, player.getBlockPos(), world);

            boolean fail = entityResult == ActionResult.FAIL || playerResult != ActionResult.FAIL;
            return fail ? ActionResult.FAIL : ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    private static boolean isInvulnerableTo(Entity source, Entity target) {
        if (!source.isPlayer() || FactionsMod.CONFIG.FRIENDLY_FIRE) return false;

        User sourceUser = User.get(source.getUuid());
        User targetUser = User.get(target.getUuid());

        if (!sourceUser.isInFaction() || !targetUser.isInFaction()) {
            return false;
        }

        if (sourceUser.getFaction() == targetUser.getFaction()) {
            return true;
        }

        if (Relationship.get(sourceUser.getFaction().getID(), targetUser.getFaction().getID()).mutuallyAllies()) {
            return true;
        }

        return false;
    }
}
