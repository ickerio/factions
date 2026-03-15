package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship.Permissions;
import io.icker.factions.api.persistents.User;
import io.icker.factions.core.InteractionsUtil.InteractionsUtilActions;
import io.icker.factions.mixin.ItemInvoker;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class InteractionManager {
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(InteractionManager::onBreakBlock);
        PlayerEvents.EXPLODE_BLOCK.register(InteractionManager::onExplodeBlock);
        PlayerEvents.EXPLODE_DAMAGE.register(InteractionManager::onExplodeDamage);
        UseBlockCallback.EVENT.register(InteractionManager::onUseBlock);
        UseItemCallback.EVENT.register(InteractionManager::onUseBucket);
        AttackEntityCallback.EVENT.register(InteractionManager::onAttackEntity);
        PlayerEvents.IS_INVULNERABLE.register(InteractionManager::isInvulnerableTo);
        PlayerEvents.USE_ENTITY.register(InteractionManager::onUseEntity);
        PlayerEvents.USE_INVENTORY.register(InteractionManager::onUseInventory);
        PlayerEvents.PLACE_BLOCK.register(InteractionManager::onPlaceBlock);
    }

    private static boolean onBreakBlock(
            Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        boolean result =
                checkPermissions(player, pos, world, Permissions.BREAK_BLOCKS)
                        == InteractionResult.FAIL;
        if (result) {
            InteractionsUtil.warn((ServerPlayer) player, InteractionsUtilActions.BREAK_BLOCKS);
        }
        return !result;
    }

    private static InteractionResult onExplodeBlock(
            Explosion explosion, BlockGetter world, BlockPos pos, BlockState state) {
        if (explosion.getIndirectSourceEntity() != null
                && explosion.getIndirectSourceEntity() instanceof Player) {
            InteractionResult result =
                    checkPermissions(
                            (Player) explosion.getIndirectSourceEntity(),
                            pos,
                            explosion.level(),
                            Permissions.BREAK_BLOCKS);
            if (result == InteractionResult.FAIL) {
                InteractionsUtil.warn(
                        (ServerPlayer) explosion.getIndirectSourceEntity(),
                        InteractionsUtilActions.BREAK_BLOCKS);
            }
            return result;
        } else {
            if (!FactionsMod.CONFIG.BLOCK_TNT) return InteractionResult.PASS;

            String dimension = explosion.level().dimension().identifier().toString();
            ChunkPos chunkPosition = explosion.level().getChunk(pos).getPos();

            Claim claim = Claim.get(chunkPosition.x, chunkPosition.z, dimension);
            if (claim == null) return InteractionResult.PASS;

            Faction claimFaction = claim.getFaction();

            if (claimFaction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT
                    > claimFaction.getPower()) {
                return InteractionResult.PASS;
            }

            if (claimFaction.guest_permissions.contains(Permissions.BREAK_BLOCKS)) {
                return InteractionResult.PASS;
            }

            return InteractionResult.FAIL;
        }
    }

    private static InteractionResult onExplodeDamage(Explosion explosion, Entity entity) {
        if (explosion.getIndirectSourceEntity() != null
                && explosion.getIndirectSourceEntity() instanceof Player) {
            InteractionResult result =
                    checkPermissions(
                            (Player) explosion.getIndirectSourceEntity(),
                            entity.blockPosition(),
                            explosion.level(),
                            Permissions.ATTACK_ENTITIES);
            if (result == InteractionResult.FAIL) {
                InteractionsUtil.warn(
                        (ServerPlayer) explosion.getIndirectSourceEntity(),
                        InteractionsUtilActions.BREAK_BLOCKS);
            }
            return result;
        } else {
            if (!FactionsMod.CONFIG.BLOCK_TNT) return InteractionResult.PASS;

            String dimension = explosion.level().dimension().identifier().toString();
            ChunkPos chunkPosition = explosion.level().getChunk(entity.blockPosition()).getPos();

            Claim claim = Claim.get(chunkPosition.x, chunkPosition.z, dimension);
            if (claim == null) return InteractionResult.PASS;

            Faction claimFaction = claim.getFaction();

            if (claimFaction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT
                    > claimFaction.getPower()) {
                return InteractionResult.PASS;
            }

            if (claimFaction.guest_permissions.contains(Permissions.ATTACK_ENTITIES)) {
                return InteractionResult.PASS;
            }

            return InteractionResult.FAIL;
        }
    }

    private static InteractionResult onUseBlock(
            Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);

        BlockPos hitPos = hitResult.getBlockPos();
        if (checkPermissions(player, hitPos, world, Permissions.USE_BLOCKS)
                == InteractionResult.FAIL) {
            InteractionsUtil.warn((ServerPlayer) player, InteractionsUtilActions.USE_BLOCKS);
            InteractionsUtil.sync((ServerPlayer) player, stack, hand);
            return InteractionResult.FAIL;
        }

        BlockPos placePos = hitPos.offset(hitResult.getDirection().getUnitVec3i());
        if (checkPermissions(player, placePos, world, Permissions.USE_BLOCKS)
                == InteractionResult.FAIL) {
            InteractionsUtil.warn((ServerPlayer) player, InteractionsUtilActions.USE_BLOCKS);
            InteractionsUtil.sync((ServerPlayer) player, stack, hand);
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onPlaceBlock(UseOnContext context) {
        if (checkPermissions(
                        context.getPlayer(),
                        context.getClickedPos(),
                        context.getLevel(),
                        Permissions.PLACE_BLOCKS)
                == InteractionResult.FAIL) {
            InteractionsUtil.warn(
                    (ServerPlayer) context.getPlayer(), InteractionsUtilActions.PLACE_BLOCKS);
            InteractionsUtil.sync(
                    (ServerPlayer) context.getPlayer(), context.getItemInHand(), context.getHand());
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onUseBucket(Player player, Level world, InteractionHand hand) {
        Item item = player.getItemInHand(hand).getItem();

        if (item instanceof BucketItem) {
            InteractionResult playerResult =
                    checkPermissions(
                            player, player.blockPosition(), world, Permissions.PLACE_BLOCKS);
            if (playerResult == InteractionResult.FAIL) {
                InteractionsUtil.warn(
                        (ServerPlayer) player, InteractionsUtilActions.PLACE_OR_PICKUP_LIQUIDS);
                InteractionsUtil.sync((ServerPlayer) player, player.getItemInHand(hand), hand);
                return InteractionResult.FAIL;
            }

            Fluid fluid = ((BucketItem) item).getContent();
            net.minecraft.world.level.ClipContext.Fluid handling =
                    fluid == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;

            BlockHitResult raycastResult = ItemInvoker.raycast(world, player, handling);

            if (raycastResult.getType() != BlockHitResult.Type.MISS) {
                BlockPos raycastPos = raycastResult.getBlockPos();
                if (checkPermissions(player, raycastPos, world, Permissions.PLACE_BLOCKS)
                        == InteractionResult.FAIL) {
                    InteractionsUtil.warn(
                            (ServerPlayer) player, InteractionsUtilActions.PLACE_OR_PICKUP_LIQUIDS);
                    InteractionsUtil.sync((ServerPlayer) player, player.getItemInHand(hand), hand);
                    return InteractionResult.FAIL;
                }

                BlockPos placePos = raycastPos.offset(raycastResult.getDirection().getUnitVec3i());
                if (checkPermissions(player, placePos, world, Permissions.PLACE_BLOCKS)
                        == InteractionResult.FAIL) {
                    InteractionsUtil.warn(
                            (ServerPlayer) player, InteractionsUtilActions.PLACE_OR_PICKUP_LIQUIDS);
                    InteractionsUtil.sync((ServerPlayer) player, player.getItemInHand(hand), hand);
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onAttackEntity(
            Player player,
            Level world,
            InteractionHand hand,
            Entity entity,
            EntityHitResult hitResult) {
        if (entity != null
                && checkPermissions(
                                player, entity.blockPosition(), world, Permissions.ATTACK_ENTITIES)
                        == InteractionResult.FAIL) {
            InteractionsUtil.warn((ServerPlayer) player, InteractionsUtilActions.ATTACK_ENTITIES);
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onUseEntity(Player player, Entity entity, Level world) {
        BlockPos pos;
        if (entity == null) {
            pos = player.blockPosition();
        } else {
            pos = entity.blockPosition();
        }

        if (checkPermissions(player, pos, world, Permissions.USE_ENTITIES)
                == InteractionResult.FAIL) {
            InteractionsUtil.warn((ServerPlayer) player, InteractionsUtilActions.USE_ENTITIES);
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult onUseInventory(Player player, BlockPos pos, Level world) {
        if (checkPermissions(player, pos, world, Permissions.USE_INVENTORIES)
                == InteractionResult.FAIL) {
            InteractionsUtil.warn((ServerPlayer) player, InteractionsUtilActions.USE_INVENTORY);
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult isInvulnerableTo(Entity source, Entity target) {
        if (!source.isAlwaysTicking() || FactionsMod.CONFIG.FRIENDLY_FIRE)
            return InteractionResult.PASS;

        User sourceUser = User.get(source.getUUID());
        User targetUser = User.get(target.getUUID());

        if (!sourceUser.isInFaction() || !targetUser.isInFaction()) {
            return InteractionResult.PASS;
        }

        Faction sourceFaction = sourceUser.getFaction();
        Faction targetFaction = targetUser.getFaction();

        if (sourceFaction.equals(targetFaction)) {
            return InteractionResult.SUCCESS;
        }

        if (sourceFaction.isMutualAllies(targetFaction.getID())) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult checkPermissions(
            Player player, BlockPos position, Level world, Permissions permission) {
        if (!FactionsMod.CONFIG.CLAIM_PROTECTION) {
            return InteractionResult.PASS;
        }

        User user = User.get(player.getUUID());
        if (player.permissions()
                        .hasPermission(
                                new Permission.HasCommandLevel(
                                        PermissionLevel.byId(
                                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL)))
                && user.bypass) {
            return InteractionResult.PASS;
        }

        String dimension = world.dimension().identifier().toString();
        ChunkPos chunkPosition = world.getChunk(position).getPos();

        Claim claim = Claim.get(chunkPosition.x, chunkPosition.z, dimension);
        if (claim == null) return InteractionResult.PASS;

        Faction claimFaction = claim.getFaction();

        if (claimFaction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT
                > claimFaction.getPower()) {
            return InteractionResult.PASS;
        }

        if (!user.isInFaction()) {
            return claimFaction.guest_permissions.contains(permission)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL;
        }

        Faction userFaction = user.getFaction();

        if (claimFaction.equals(userFaction)
                && (getRankLevel(claim.accessLevel) <= getRankLevel(user.rank)
                        || (user.rank == User.Rank.GUEST
                                && claimFaction.guest_permissions.contains(permission)
                                && claim.accessLevel == User.Rank.MEMBER))) {
            return InteractionResult.SUCCESS;
        }

        if (FactionsMod.CONFIG.RELATIONSHIPS.ALLY_OVERRIDES_PERMISSIONS
                && claimFaction.isMutualAllies(userFaction.getID())
                && claim.accessLevel == User.Rank.MEMBER) {
            return InteractionResult.SUCCESS;
        }

        if (claimFaction.getRelationship(userFaction.getID()).permissions.contains(permission)
                && claim.accessLevel == User.Rank.MEMBER) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private static int getRankLevel(User.Rank rank) {
        switch (rank) {
            case OWNER -> {
                return 3;
            }
            case LEADER -> {
                return 2;
            }
            case COMMANDER -> {
                return 1;
            }
            case MEMBER -> {
                return 0;
            }
            case GUEST -> {
                return -1;
            }
            default -> {
                return -2;
            }
        }
    }
}
