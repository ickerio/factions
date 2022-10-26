package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Faction;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
* Events related to player actions
*/
public class PlayerEvents {
    /**
     * Called when a player tries to interact with an entity
     */
    public static final Event<UseEntity> USE_ENTITY = EventFactory.createArrayBacked(UseEntity.class, callbacks -> (source, target, world) -> {
        for (UseEntity callback : callbacks) {
            ActionResult result = callback.onUseEntity(source, target, world);
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    /**
     * Called when a player is attacked and decides whether to allow the hit
     */
    public static final Event<IsInvulnerable> IS_INVULNERABLE = EventFactory.createArrayBacked(IsInvulnerable.class, callbacks -> (source, target) -> {
        for (IsInvulnerable callback : callbacks) {
            ActionResult result = callback.isInvulnerable(source, target);
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    /**
     * Called when a player moves
     */
    public static final Event<Move> ON_MOVE = EventFactory.createArrayBacked(Move.class, callbacks -> (player) -> {
        for (Move callback : callbacks) {
            callback.onMove(player);
        }
    });

    /**
     * Called when a player dies
     */
    public static final Event<PlayerDeath> ON_PLAYER_DEATH = EventFactory.createArrayBacked(PlayerDeath.class, callbacks -> (player, source, killedByPlayer) -> {
        for (PlayerDeath callback : callbacks) {
            callback.onPlayerDeath(player, source, killedByPlayer);
        }
    });

    /**
     * Called on a power reward will be given
     */
    public static final Event<PowerTick> ON_POWER_TICK = EventFactory.createArrayBacked(PowerTick.class, callbacks -> (player) -> {
        for (PowerTick callback : callbacks) {
            callback.onPowerTick(player);
        }
    });

    /**
     * Called when a player attempts to open a safe
     */
    public static final Event<OpenSafe> OPEN_SAFE = EventFactory.createArrayBacked(OpenSafe.class, callbacks -> (player, faction) -> {
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
    public interface IsInvulnerable {
        ActionResult isInvulnerable(Entity source, Entity target);
    }

    @FunctionalInterface
    public interface Move {
        void onMove(ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface PlayerDeath {
        void onPlayerDeath(ServerPlayerEntity player, DamageSource source, boolean killedByPlayer);
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
