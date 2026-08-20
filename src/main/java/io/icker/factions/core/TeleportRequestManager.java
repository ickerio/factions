package io.icker.factions.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state manager for /factions tp requests and per-player teleport cooldowns.
 *
 * <p>Not persisted: pending requests and cooldowns are ephemeral by design and are dropped on
 * server restart or player disconnect.
 *
 * <p>Time-sensitive methods accept {@code nowMillis} as a parameter rather than calling {@link
 * System#currentTimeMillis()} internally so expiry and cooldown behaviour can be unit-tested
 * without sleeping.
 */
public final class TeleportRequestManager {
    /** A single pending teleport request from {@code requesterID} to some target. */
    public record Request(UUID requesterID, long createdAtMillis) {}

    /** target uuid -> pending request (one slot per target; newer overwrites older). */
    private static final Map<UUID, Request> pending = new ConcurrentHashMap<>();

    /** player uuid -> millis of last successful teleport. */
    private static final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    private TeleportRequestManager() {}

    /** Store a pending request keyed by target; overwrites any prior request for that target. */
    public static void request(UUID targetID, UUID requesterID, long nowMillis) {
        pending.put(targetID, new Request(requesterID, nowMillis));
    }

    /** Return the pending request for {@code targetID}, or {@code null} if none. */
    public static Request peek(UUID targetID) {
        return pending.get(targetID);
    }

    /** Pure predicate: has {@code request} exceeded the expiry window at {@code nowMillis}? */
    public static boolean isExpired(Request request, long nowMillis, int expirySeconds) {
        return nowMillis - request.createdAtMillis() >= expirySeconds * 1000L;
    }

    /** Return and clear the pending request for {@code targetID}; {@code null} if none. */
    public static Request consume(UUID targetID) {
        return pending.remove(targetID);
    }

    /** Remove any pending request for {@code targetID}. */
    public static void deny(UUID targetID) {
        pending.remove(targetID);
    }

    /** True if {@code playerID} teleported inside the cooldown window ending at {@code nowMillis}. */
    public static boolean isOnCooldown(UUID playerID, long nowMillis, int cooldownSeconds) {
        Long last = cooldown.get(playerID);
        if (last == null) return false;
        return nowMillis - last < cooldownSeconds * 1000L;
    }

    /** Milliseconds of cooldown remaining for {@code playerID}, clamped to 0. */
    public static long cooldownRemainingMillis(UUID playerID, long nowMillis, int cooldownSeconds) {
        Long last = cooldown.get(playerID);
        if (last == null) return 0L;
        long remaining = (last + cooldownSeconds * 1000L) - nowMillis;
        return Math.max(0L, remaining);
    }

    /** Record that {@code playerID} completed a teleport at {@code nowMillis}. */
    public static void markTeleported(UUID playerID, long nowMillis) {
        cooldown.put(playerID, nowMillis);
    }

    /**
     * Drop all state associated with a disconnecting player:
     *
     * <ul>
     *   <li>any pending request where they are the target,
     *   <li>any pending request where they are the requester (otherwise the map leaks),
     *   <li>their cooldown entry.
     * </ul>
     */
    public static void clearPlayer(UUID playerID) {
        pending.remove(playerID);
        pending.entrySet().removeIf(e -> e.getValue().requesterID().equals(playerID));
        cooldown.remove(playerID);
    }
}
