package io.icker.factions.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.icker.factions.core.TradeRequestManager.TradeRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Pure-state unit tests for {@link TradeRequestManager}.
 *
 * <p>{@code TradeRequestManager} holds its pending-request table in a static
 * {@link java.util.concurrent.ConcurrentHashMap}, so every test allocates fresh
 * {@link UUID}s in {@link #resetState()} and defensively calls
 * {@link TradeRequestManager#clearFactionRequests(UUID)} on each of them to
 * guarantee we never see state leak from an earlier test method.
 */
class TradeRequestManagerTest {
    private static final int EXPIRY_SECONDS = 60;
    private static final long T0 = 1_000_000L;

    private UUID targetFaction;
    private UUID requesterFaction;
    private UUID requesterPlayer;
    private UUID otherFaction;

    @BeforeEach
    void resetState() {
        targetFaction = UUID.randomUUID();
        requesterFaction = UUID.randomUUID();
        requesterPlayer = UUID.randomUUID();
        otherFaction = UUID.randomUUID();

        // Defensive: scrub any lingering static state on the UUIDs this test will use.
        TradeRequestManager.clearFactionRequests(targetFaction);
        TradeRequestManager.clearFactionRequests(requesterFaction);
        TradeRequestManager.clearFactionRequests(otherFaction);
    }

    @Test
    void requestAndPeek() {
        // Given a trade request stored against `targetFaction`
        TradeRequestManager.request(targetFaction, requesterFaction, requesterPlayer, T0);

        // When we peek the target's slot
        TradeRequest req = TradeRequestManager.peek(targetFaction);

        // Then we get back the exact triple we stored
        assertNotNull(req, "peek should return the stored request");
        assertEquals(requesterFaction, req.requesterFactionID(), "requester faction UUID must round-trip");
        assertEquals(requesterPlayer, req.requesterPlayerID(), "requester player UUID must round-trip");
        assertEquals(T0, req.createdAtMillis(), "createdAt millis must round-trip");
    }

    @Test
    void peekReturnsNullWhenNone() {
        // Given no request has been made for `targetFaction`

        // When we peek
        TradeRequest req = TradeRequestManager.peek(targetFaction);

        // Then the slot is empty
        assertNull(req, "peek with no pending request must return null");
    }

    @Test
    void consumeRemovesRequest() {
        // Given a pending request
        TradeRequestManager.request(targetFaction, requesterFaction, requesterPlayer, T0);

        // When we consume it
        TradeRequest consumed = TradeRequestManager.consume(targetFaction);

        // Then consume returns the request and the slot is empty on the next peek
        assertNotNull(consumed, "consume should return the stored request");
        assertEquals(requesterFaction, consumed.requesterFactionID());
        assertEquals(requesterPlayer, consumed.requesterPlayerID());
        assertNull(TradeRequestManager.peek(targetFaction), "second peek after consume must be null");
    }

    @Test
    void denyRemovesRequest() {
        // Given a pending request
        TradeRequestManager.request(targetFaction, requesterFaction, requesterPlayer, T0);

        // When we deny it
        TradeRequestManager.deny(targetFaction);

        // Then the slot is empty
        assertNull(TradeRequestManager.peek(targetFaction), "deny must clear the pending request");
    }

    @Test
    void isExpiredFalseBeforeExpiry() {
        // Given a request created at T0 with a 60s expiry window
        TradeRequestManager.request(targetFaction, requesterFaction, requesterPlayer, T0);
        TradeRequest req = TradeRequestManager.peek(targetFaction);

        // When only 30s have elapsed (well inside the 60s window)
        long now = T0 + 30_000L;

        // Then the request is NOT expired
        assertFalse(
                TradeRequestManager.isExpired(req, now, EXPIRY_SECONDS),
                "30s elapsed against a 60s expiry must not be expired");
    }

    @Test
    void isExpiredTrueAfterExpiry() {
        // Given a request created at T0 with a 60s expiry window
        TradeRequestManager.request(targetFaction, requesterFaction, requesterPlayer, T0);
        TradeRequest req = TradeRequestManager.peek(targetFaction);

        // When 61s have elapsed (past the 60s window)
        long now = T0 + 61_000L;

        // Then the request IS expired
        assertTrue(
                TradeRequestManager.isExpired(req, now, EXPIRY_SECONDS),
                "61s elapsed against a 60s expiry must be expired");
    }

    @Test
    void requestOverwritesPrevious() {
        // Given two requests fired against the same target from two different requesters
        UUID firstRequesterFaction = UUID.randomUUID();
        UUID firstRequesterPlayer = UUID.randomUUID();
        UUID secondRequesterFaction = UUID.randomUUID();
        UUID secondRequesterPlayer = UUID.randomUUID();
        // Scrub any state on the ad-hoc UUIDs.
        TradeRequestManager.clearFactionRequests(firstRequesterFaction);
        TradeRequestManager.clearFactionRequests(secondRequesterFaction);

        TradeRequestManager.request(targetFaction, firstRequesterFaction, firstRequesterPlayer, T0);
        TradeRequestManager.request(
                targetFaction, secondRequesterFaction, secondRequesterPlayer, T0 + 5_000L);

        // When we peek
        TradeRequest req = TradeRequestManager.peek(targetFaction);

        // Then only the newer request is kept (single slot per target)
        assertNotNull(req);
        assertEquals(
                secondRequesterFaction,
                req.requesterFactionID(),
                "the newer request must overwrite the older for the same target");
        assertEquals(
                secondRequesterPlayer,
                req.requesterPlayerID(),
                "the newer request's player must overwrite the older one");
        assertEquals(
                T0 + 5_000L,
                req.createdAtMillis(),
                "createdAt must reflect the newer request");
    }

    @Test
    void clearFactionRequestsRemovesTargetSide() {
        // Given a request pending against `targetFaction`
        TradeRequestManager.request(targetFaction, requesterFaction, requesterPlayer, T0);

        // When we clear the target faction
        TradeRequestManager.clearFactionRequests(targetFaction);

        // Then the request keyed by the target is gone
        assertNull(
                TradeRequestManager.peek(targetFaction),
                "clearFactionRequests(target) must remove the pending request keyed by that target");
    }

    @Test
    void clearFactionRequestsRemovesRequesterSide() {
        // Given the requester has outstanding requests against TWO different targets
        // (this is the leak vector: the map is keyed by target, so simply removing the
        // requester UUID as a key would miss both entries).
        TradeRequestManager.request(targetFaction, requesterFaction, requesterPlayer, T0);
        TradeRequestManager.request(otherFaction, requesterFaction, requesterPlayer, T0);

        // When we clear the requester faction
        TradeRequestManager.clearFactionRequests(requesterFaction);

        // Then both target-keyed slots that referenced the requester are cleared
        assertNull(
                TradeRequestManager.peek(targetFaction),
                "clearFactionRequests(requester) must remove requests where requester is the sender (target-keyed)");
        assertNull(
                TradeRequestManager.peek(otherFaction),
                "clearFactionRequests(requester) must remove requests where requester is the sender (otherFaction-keyed)");
    }
}
