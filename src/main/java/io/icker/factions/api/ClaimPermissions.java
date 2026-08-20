package io.icker.factions.api;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.GuestGrant;
import io.icker.factions.api.persistents.Relationship.Permissions;
import io.icker.factions.api.persistents.User;
import io.icker.factions.core.InteractionsUtil;
import io.icker.factions.core.InteractionsUtil.InteractionsUtilActions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ClaimPermissions {
    private ClaimPermissions() {}

    public static boolean canPlaceBlock(ServerPlayer player, BlockPos pos) {
        return canPlaceBlock(player, pos, ItemStack.EMPTY);
    }

    public static boolean canPlaceBlock(ServerPlayer player, BlockPos pos, ItemStack usedItem) {
        return check(player, pos, player.level(), Permissions.PLACE_BLOCKS, false, usedItem)
                != InteractionResult.FAIL;
    }

    public static boolean tryPlaceBlock(ServerPlayer player, BlockPos pos) {
        return tryPlaceBlock(player, pos, ItemStack.EMPTY);
    }

    public static boolean tryPlaceBlock(ServerPlayer player, BlockPos pos, ItemStack usedItem) {
        boolean allowed =
                check(player, pos, player.level(), Permissions.PLACE_BLOCKS, true, usedItem)
                        != InteractionResult.FAIL;
        if (!allowed) {
            InteractionsUtil.warn(player, InteractionsUtilActions.PLACE_BLOCKS);
        }
        return allowed;
    }

    public static InteractionResult check(
            Player player, BlockPos position, Level world, Permissions permission) {
        return check(player, position, world, permission, false, ItemStack.EMPTY);
    }

    public static InteractionResult check(
            Player player,
            BlockPos position,
            Level world,
            Permissions permission,
            boolean consumeQuota) {
        return check(player, position, world, permission, consumeQuota, ItemStack.EMPTY);
    }

    public static InteractionResult check(
            Player player,
            BlockPos position,
            Level world,
            Permissions permission,
            boolean consumeQuota,
            ItemStack usedItem) {
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

        Claim claim = Claim.get(world, position);
        if (claim == null) return InteractionResult.PASS;

        Faction claimFaction = claim.getFaction();
        if (claimFaction.getClaimCount() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT
                > claimFaction.getPower()) {
            return InteractionResult.PASS;
        }

        if (!user.isInFaction()) {
            if (isRestrictedForGuests(usedItem)) {
                return InteractionResult.FAIL;
            }

            if (claimFaction.guest_permissions.contains(permission)) {
                return InteractionResult.SUCCESS;
            }

            GuestGrant grant = GuestGrant.get(claimFaction.getID(), user.getID());
            if (grant != null) {
                if (permission == Permissions.BREAK_BLOCKS && grant.breakRemaining > 0) {
                    if (consumeQuota) {
                        grant.consumeBreak();
                        saveGrant(grant);
                    }
                    return InteractionResult.SUCCESS;
                }
                if (permission == Permissions.PLACE_BLOCKS && grant.placeRemaining > 0) {
                    if (consumeQuota) {
                        grant.consumePlace();
                        saveGrant(grant);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.FAIL;
        }

        Faction userFaction = user.getFaction();
        if (claimFaction.equals(userFaction)
                && (rankLevel(claim.accessLevel) <= rankLevel(user.rank)
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

    public static boolean isRestrictedForGuests(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        List<String> restricted = FactionsMod.CONFIG.GUEST_GRANT.RESTRICTED_ITEMS;
        if (restricted == null || restricted.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return restricted.contains(id);
    }

    public static boolean isNonMemberInClaim(Player player, BlockPos pos, Level world) {
        if (!FactionsMod.CONFIG.CLAIM_PROTECTION) {
            return false;
        }

        User user = User.get(player.getUUID());
        if (player.permissions()
                        .hasPermission(
                                new Permission.HasCommandLevel(
                                        PermissionLevel.byId(
                                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL)))
                && user.bypass) {
            return false;
        }

        Claim claim = Claim.get(world, pos);
        if (claim == null) return false;

        Faction claimFaction = claim.getFaction();
        if (claimFaction.getClaimCount() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT
                > claimFaction.getPower()) {
            return false;
        }

        return !user.isInFaction();
    }

    private static void saveGrant(GuestGrant grant) {
        if (grant.breakRemaining == 0 && grant.placeRemaining == 0) {
            grant.remove();
            GuestGrant.save();
        } else {
            GuestGrant.saveThrottled();
        }
    }

    private static int rankLevel(User.Rank rank) {
        return switch (rank) {
            case OWNER -> 3;
            case LEADER -> 2;
            case COMMANDER -> 1;
            case MEMBER -> 0;
            case GUEST -> -1;
        };
    }
}
