package io.icker.factions.core;

import io.icker.factions.api.events.FactionEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state manager for /f trade requests between factions.
 *
 * <p>Not persisted: pending requests are ephemeral by design and are dropped on server restart.
 *
 * <p>Time-sensitive methods accept {@code nowMillis} as a parameter rather than calling {@link
 * System#currentTimeMillis()} internally so expiry behaviour can be unit-tested without sleeping.
 */
public final class TradeRequestManager {
    /** A single pending trade request from {@code requesterFactionID} (initiated by {@code requesterPlayerID}) to some target faction. */
    public record TradeRequest(UUID requesterFactionID, UUID requesterPlayerID, long createdAtMillis) {}

    /** target faction uuid -> pending trade request (one slot per target faction; newer overwrites older). */
    private static final Map<UUID, TradeRequest> pending = new ConcurrentHashMap<>();

    private TradeRequestManager() {}

    /** Register event listeners so pending state is cleared when a faction disbands. */
    public static void register() {
        FactionEvents.DISBAND.register(faction -> clearFactionRequests(faction.getID()));
    }

    /** Store a pending trade request keyed by target faction; overwrites any prior request for that target. */
    public static void request(UUID targetFactionID, UUID requesterFactionID, UUID requesterPlayerID, long nowMillis) {
        pending.put(targetFactionID, new TradeRequest(requesterFactionID, requesterPlayerID, nowMillis));
    }

    /** Return the pending trade request for {@code targetFactionID}, or {@code null} if none. */
    public static TradeRequest peek(UUID targetFactionID) {
        return pending.get(targetFactionID);
    }

    /** Return and clear the pending trade request for {@code targetFactionID}; {@code null} if none. */
    public static TradeRequest consume(UUID targetFactionID) {
        return pending.remove(targetFactionID);
    }

    /** Remove any pending trade request for {@code targetFactionID}. */
    public static void deny(UUID targetFactionID) {
        pending.remove(targetFactionID);
    }

    /** Pure predicate: has {@code req} exceeded the expiry window at {@code nowMillis}? */
    public static boolean isExpired(TradeRequest req, long nowMillis, int expirySeconds) {
        return nowMillis - req.createdAtMillis() >= expirySeconds * 1000L;
    }

    /**
     * Drop all state associated with a disbanding faction:
     *
     * <ul>
     *   <li>any pending request where they are the target faction,
     *   <li>any pending request where they are the requester faction (otherwise the map leaks).
     * </ul>
     */
    public static void clearFactionRequests(UUID factionID) {
        pending.remove(factionID);
        pending.entrySet().removeIf(e -> e.getValue().requesterFactionID().equals(factionID));
    }
}
