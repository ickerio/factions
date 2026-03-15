package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Faction;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Events related to player actions */
public class PlayerEvents {
    /** Called when a player tries to interact with an entity */
    public static final Event<UseEntity> USE_ENTITY =
            EventFactory.createArrayBacked(
                    UseEntity.class,
                    callbacks ->
                            (source, target, world) -> {
                                for (UseEntity callback : callbacks) {
                                    InteractionResult result =
                                            callback.onUseEntity(source, target, world);
                                    if (result != InteractionResult.PASS) {
                                        return result;
                                    }
                                }
                                return InteractionResult.PASS;
                            });

    public static final Event<PlaceBlock> PLACE_BLOCK =
            EventFactory.createArrayBacked(
                    PlaceBlock.class,
                    callbacks ->
                            (context) -> {
                                for (PlaceBlock callback : callbacks) {
                                    InteractionResult result = callback.onPlaceBlock(context);
                                    if (result != InteractionResult.PASS) {
                                        return result;
                                    }
                                }
                                return InteractionResult.PASS;
                            });

    public static final Event<ExplodeBlock> EXPLODE_BLOCK =
            EventFactory.createArrayBacked(
                    ExplodeBlock.class,
                    callbacks ->
                            (explosion, world, pos, state) -> {
                                for (ExplodeBlock callback : callbacks) {
                                    InteractionResult result =
                                            callback.onExplodeBlock(explosion, world, pos, state);
                                    if (result != InteractionResult.PASS) {
                                        return result;
                                    }
                                }
                                return InteractionResult.PASS;
                            });

    public static final Event<ExplodeDamage> EXPLODE_DAMAGE =
            EventFactory.createArrayBacked(
                    ExplodeDamage.class,
                    callbacks ->
                            (explosion, entity) -> {
                                for (ExplodeDamage callback : callbacks) {
                                    InteractionResult result =
                                            callback.onExplodeDamage(explosion, entity);
                                    if (result != InteractionResult.PASS) {
                                        return result;
                                    }
                                }
                                return InteractionResult.PASS;
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
                                    InteractionResult result =
                                            callback.onUseInventory(source, pos, world);
                                    if (result != InteractionResult.PASS) {
                                        return result;
                                    }
                                }
                                return InteractionResult.PASS;
                            });

    /** Called when a player is attacked and decides whether to allow the hit */
    public static final Event<IsInvulnerable> IS_INVULNERABLE =
            EventFactory.createArrayBacked(
                    IsInvulnerable.class,
                    callbacks ->
                            (source, target) -> {
                                for (IsInvulnerable callback : callbacks) {
                                    InteractionResult result =
                                            callback.isInvulnerable(source, target);
                                    if (result != InteractionResult.PASS) {
                                        return result;
                                    }
                                }
                                return InteractionResult.PASS;
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
                                    InteractionResult result = callback.onOpenSafe(player, faction);
                                    if (result != InteractionResult.PASS) {
                                        return result;
                                    }
                                }
                                return InteractionResult.PASS;
                            });

    @FunctionalInterface
    public interface UseEntity {
        InteractionResult onUseEntity(ServerPlayer player, Entity entity, Level world);
    }

    @FunctionalInterface
    public interface PlaceBlock {
        InteractionResult onPlaceBlock(UseOnContext context);
    }

    @FunctionalInterface
    public interface ExplodeBlock {
        InteractionResult onExplodeBlock(
                Explosion explosion, BlockGetter world, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface ExplodeDamage {
        InteractionResult onExplodeDamage(Explosion explosion, Entity entity);
    }

    @FunctionalInterface
    public interface UseInventory {
        InteractionResult onUseInventory(Player player, BlockPos pos, Level world);
    }

    @FunctionalInterface
    public interface IsInvulnerable {
        InteractionResult isInvulnerable(Entity source, Entity target);
    }

    @FunctionalInterface
    public interface Move {
        void onMove(ServerPlayer player);
    }

    @FunctionalInterface
    public interface KilledByPlayer {
        void onKilledByPlayer(ServerPlayer player, DamageSource source);
    }

    @FunctionalInterface
    public interface PowerTick {
        void onPowerTick(ServerPlayer player);
    }

    @FunctionalInterface
    public interface OpenSafe {
        InteractionResult onOpenSafe(Player player, Faction faction);
    }
}
