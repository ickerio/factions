package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Faction;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

/** Events related to player actions */
public class PlayerEvents {
    /** Called when a player tries to interact with an entity */
    public static final Event<UseEntity> USE_ENTITY =
            EventFactory.createArrayBacked(
                    UseEntity.class,
                    callbacks ->
                            (source, target, world) -> {
                                for (UseEntity callback : callbacks) {
                                    ActionResult result =
                                            callback.onUseEntity(source, target, world);
                                    if (result != ActionResult.PASS) {
                                        return result;
                                    }
                                }
                                return ActionResult.PASS;
                            });

    public static final Event<PlaceBlock> PLACE_BLOCK =
            EventFactory.createArrayBacked(
                    PlaceBlock.class,
                    callbacks ->
                            (context) -> {
                                for (PlaceBlock callback : callbacks) {
                                    ActionResult result = callback.onPlaceBlock(context);
                                    if (result != ActionResult.PASS) {
                                        return result;
                                    }
                                }
                                return ActionResult.PASS;
                            });

    public static final Event<ExplodeBlock> EXPLODE_BLOCK =
            EventFactory.createArrayBacked(
                    ExplodeBlock.class,
                    callbacks ->
                            (explosion, world, pos, state) -> {
                                for (ExplodeBlock callback : callbacks) {
                                    ActionResult result = callback.onExplodeBlock(explosion, world, pos, state);
                                    if (result != ActionResult.PASS) {
                                        return result;
                                    }
                                }
                                return ActionResult.PASS;
                            });

    public static final Event<ExplodeDamage> EXPLODE_DAMAGE =
            EventFactory.createArrayBacked(
                    ExplodeDamage.class,
                    callbacks ->
                            (explosion, entity) -> {
                                for (ExplodeDamage callback : callbacks) {
                                    ActionResult result = callback.onExplodeDamage(explosion, entity);
                                    if (result != ActionResult.PASS) {
                                        return result;
                                    }
                                }
                                return ActionResult.PASS;
                            });

    /**
     * Called when a player tries to use a block that has an inventory (uses the locking mechanism)
     */
    public static final Event<UseInventory> USE_INVENTORY =
            EventFactory.createArrayBacked(
                    UseInventory.class,
                    callbacks ->
                            (source, pos, world) -> {
                                for (UseInventory callback : callbacks) {
                                    ActionResult result =
                                            callback.onUseInventory(source, pos, world);
                                    if (result != ActionResult.PASS) {
                                        return result;
                                    }
                                }
                                return ActionResult.PASS;
                            });

    /** Called when a player is attacked and decides whether to allow the hit */
    public static final Event<IsInvulnerable> IS_INVULNERABLE =
            EventFactory.createArrayBacked(
                    IsInvulnerable.class,
                    callbacks ->
                            (source, target) -> {
                                for (IsInvulnerable callback : callbacks) {
                                    ActionResult result = callback.isInvulnerable(source, target);
                                    if (result != ActionResult.PASS) {
                                        return result;
                                    }
                                }
                                return ActionResult.PASS;
                            });

    /** Called when a player moves */
    public static final Event<Move> ON_MOVE =
            EventFactory.createArrayBacked(
                    Move.class,
                    callbacks ->
                            (player) -> {
                                for (Move callback : callbacks) {
                                    callback.onMove(player);
                                }
                            });

    /** Called when a player is killed by another player */
    public static final Event<KilledByPlayer> ON_KILLED_BY_PLAYER =
            EventFactory.createArrayBacked(
                    KilledByPlayer.class,
                    callbacks ->
                            (player, source) -> {
                                for (KilledByPlayer callback : callbacks) {
                                    callback.onKilledByPlayer(player, source);
                                }
                            });

    /** Called on a power reward will be given */
    public static final Event<PowerTick> ON_POWER_TICK =
            EventFactory.createArrayBacked(
                    PowerTick.class,
                    callbacks ->
                            (player) -> {
                                for (PowerTick callback : callbacks) {
                                    callback.onPowerTick(player);
                                }
                            });

    /** Called when a player attempts to open a safe */
    public static final Event<OpenSafe> OPEN_SAFE =
            EventFactory.createArrayBacked(
                    OpenSafe.class,
                    callbacks ->
                            (player, faction) -> {
                                for (OpenSafe callback : callbacks) {
                                    ActionResult result = callback.onOpenSafe(player, faction);
                                    if (result != ActionResult.PASS) {
                                        return result;
                                    }
                                }
                                return ActionResult.PASS;
                            });

    @FunctionalInterface
    public interface UseEntity {
        ActionResult onUseEntity(ServerPlayerEntity player, Entity entity, World world);
    }

    @FunctionalInterface
    public interface PlaceBlock {
        ActionResult onPlaceBlock(ItemUsageContext context);
    }

    @FunctionalInterface
    public interface ExplodeBlock {
        ActionResult onExplodeBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface ExplodeDamage {
        ActionResult onExplodeDamage(Explosion explosion, Entity entity);
    }

    @FunctionalInterface
    public interface UseInventory {
        ActionResult onUseInventory(PlayerEntity player, BlockPos pos, World world);
    }

    @FunctionalInterface
    public interface IsInvulnerable {
        ActionResult isInvulnerable(Entity source, Entity target);
    }

    @FunctionalInterface
    public interface Move {
        void onMove(ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface KilledByPlayer {
        void onKilledByPlayer(ServerPlayerEntity player, DamageSource source);
    }

    @FunctionalInterface
    public interface PowerTick {
        void onPowerTick(ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface OpenSafe {
        ActionResult onOpenSafe(PlayerEntity player, Faction faction);
    }
}
