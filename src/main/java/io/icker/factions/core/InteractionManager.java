package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.mixin.BucketItemMixin;
import io.icker.factions.mixin.ItemMixin;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.World;

public class InteractionManager {
    public static void register() {
        PlayerEvents.BREAK_BLOCK.register(InteractionManager::checkPermissions);
        PlayerEvents.USE_BLOCK.register(InteractionManager::onUseBlock);
        PlayerEvents.USE_ITEM.register(InteractionManager::onUseItem);
        AttackEntityCallback.EVENT.register(InteractionManager::onAttackEntity);
        PlayerEvents.IS_INVULNERABLE.register(InteractionManager::isInvulnerableTo);
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (checkPermissions(player, player.getBlockPos(), world) == ActionResult.FAIL) {
            return ActionResult.FAIL;
        }

        BlockPos hitPos = hitResult.getBlockPos();
        if (checkPermissions(player, hitPos, world) == ActionResult.FAIL) {
            return ActionResult.FAIL;
        }

        BlockPos placePos = hitPos.add(hitResult.getSide().getVector());
        if (checkPermissions(player, placePos, world) == ActionResult.FAIL) {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    private static ActionResult onUseItem(PlayerEntity player, World world, ItemStack stack, Hand hand) {
        Item item = stack.getItem();

        if (item instanceof BucketItem) {
            ActionResult playerResult = checkPermissions(player, player.getBlockPos(), world);
            if (playerResult == ActionResult.FAIL) {
                return ActionResult.FAIL;
            }
                
            Fluid fluid = ((BucketItemMixin) item).getFluid();
            FluidHandling handling = fluid == Fluids.EMPTY ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE;

            BlockHitResult raycastResult = ItemMixin.raycast(world, player, handling);

            if (raycastResult.getType() != BlockHitResult.Type.MISS) {
                BlockPos raycastPos = raycastResult.getBlockPos();
                if (checkPermissions(player, raycastPos, world) == ActionResult.FAIL) {
                    return ActionResult.FAIL;
                }

                BlockPos placePos = raycastPos.add(raycastResult.getSide().getVector());
                if (checkPermissions(player, placePos, world) == ActionResult.FAIL) {
                    return ActionResult.FAIL;
                }
            }
        }


        return ActionResult.PASS;
    }

    private static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (entity.isPlayer()) {
            return isInvulnerableTo(player, entity) == ActionResult.SUCCESS ? ActionResult.FAIL : ActionResult.PASS;
        }

        if (!entity.isLiving()) {
            if (checkPermissions(player, entity.getBlockPos(), world) == ActionResult.FAIL) {
                return ActionResult.FAIL;
            }

            if (checkPermissions(player, player.getBlockPos(), world) == ActionResult.FAIL) {
                return ActionResult.FAIL;
            }
        }

        return ActionResult.PASS;
    }

    private static ActionResult isInvulnerableTo(Entity source, Entity target) {
        if (!source.isPlayer() || FactionsMod.CONFIG.FRIENDLY_FIRE) return ActionResult.PASS;

        User sourceUser = User.get(source.getUuid());
        User targetUser = User.get(target.getUuid());

        if (!sourceUser.isInFaction() || !targetUser.isInFaction()) {
            return ActionResult.PASS;
        }

        Faction sourceFaction = sourceUser.getFaction();
        Faction targetFaction = targetUser.getFaction();

        if (sourceFaction.getID() == targetFaction.getID()) {
            return ActionResult.SUCCESS;
        }

        if (sourceFaction.isMutualAllies(targetFaction.getID())) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private static ActionResult checkPermissions(PlayerEntity player, BlockPos position, World world) {
        User user = User.get(player.getUuid());
        if (player.hasPermissionLevel(FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL) && user.bypass) {
            return ActionResult.PASS;
        }

        String dimension = world.getRegistryKey().getValue().toString();
        ChunkPos chunkPosition = world.getChunk(position).getPos();

        Claim claim = Claim.get(chunkPosition.x, chunkPosition.z, dimension);
        if (claim == null) return ActionResult.PASS;

        Faction claimFaction = claim.getFaction();

        if (claimFaction.getClaims().size() * FactionsMod.CONFIG.CLAIM_WEIGHT > claimFaction.getPower()) {
            return ActionResult.PASS;
        }

        if (!user.isInFaction()) {
            return ActionResult.FAIL;
        }

        Faction userFaction = user.getFaction();

        if (claimFaction == userFaction) {
            return ActionResult.PASS;
        }

        if (claimFaction.isMutualAllies(userFaction.getID())) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }
}
