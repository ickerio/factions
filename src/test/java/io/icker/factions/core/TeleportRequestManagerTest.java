package io.icker.factions.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.icker.factions.core.TeleportRequestManager.Request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class TeleportRequestManagerTest {
    private static final int EXPIRY_SECONDS = 60;
    private static final int COOLDOWN_SECONDS = 15;
    private static final long T0 = 1_000_000L;

    private UUID requester;
    private UUID target;
    private UUID other;

    @BeforeEach
    void resetState() {
        requester = UUID.randomUUID();
        target = UUID.randomUUID();
        other = UUID.randomUUID();

        // Clean any lingering static state on the UUIDs this test will use.
        TeleportRequestManager.clearPlayer(requester);
        TeleportRequestManager.clearPlayer(target);
        TeleportRequestManager.clearPlayer(other);
    }

    @Test
    void request_then_peek_returnsStoredRequesterID() {
        TeleportRequestManager.request(target, requester, T0);

        Request req = TeleportRequestManager.peek(target);

        assertNotNull(req, "peek should return the stored request");
        assertEquals(requester, req.requesterID(), "peek must return the correct requester UUID");
        assertEquals(T0, req.createdAtMillis(), "peek must return the createdAt millis it was stored with");
    }

    @Test
    void isExpired_falseJustInsideWindow() {
        TeleportRequestManager.request(target, requester, T0);
        Request req = TeleportRequestManager.peek(target);

        // one millisecond before the expiry boundary
        long now = T0 + (EXPIRY_SECONDS * 1000L) - 1;

        assertFalse(
                TeleportRequestManager.isExpired(req, now, EXPIRY_SECONDS),
                "request should NOT be expired one millisecond before the boundary");
    }

    @Test
    void isExpired_trueJustOutsideWindow() {
        TeleportRequestManager.request(target, requester, T0);
        Request req = TeleportRequestManager.peek(target);

        // one millisecond past the expiry boundary
        long now = T0 + (EXPIRY_SECONDS * 1000L) + 1;

        assertTrue(
                TeleportRequestManager.isExpired(req, now, EXPIRY_SECONDS),
                "request MUST be expired one millisecond past the boundary");
    }

    @Test
    void consume_returnsRequestAndClearsIt() {
        TeleportRequestManager.request(target, requester, T0);

        Request consumed = TeleportRequestManager.consume(target);

        assertNotNull(consumed, "consume should return the stored request");
        assertEquals(requester, consumed.requesterID());
        assertNull(TeleportRequestManager.peek(target), "second peek after consume must be null");
    }

    @Test
    void deny_clearsPendingRequest() {
        TeleportRequestManager.request(target, requester, T0);

        TeleportRequestManager.deny(target);

        assertNull(TeleportRequestManager.peek(target), "deny must clear the pending request");
    }

    @Test
    void isOnCooldown_trueImmediatelyAfterMarkTeleported() {
        TeleportRequestManager.markTeleported(requester, T0);

        assertTrue(
                TeleportRequestManager.isOnCooldown(requester, T0, COOLDOWN_SECONDS),
                "player must be on cooldown immediately after marking teleport");
    }

    @Test
    void isOnCooldown_falseAfterCooldownWindow() {
        TeleportRequestManager.markTeleported(requester, T0);

        long past = T0 + (COOLDOWN_SECONDS * 1000L);

        assertFalse(
                TeleportRequestManager.isOnCooldown(requester, past, COOLDOWN_SECONDS),
                "cooldown must lapse exactly at the boundary (nowMillis - last < window)");
    }

    @Test
    void isOnCooldown_falseWhenNoPriorTeleport() {
        assertFalse(
                TeleportRequestManager.isOnCooldown(requester, T0, COOLDOWN_SECONDS),
                "player with no teleport history must not be on cooldown");
    }

    @Test
    void cooldownRemainingMillis_decreasesAsTimeAdvances() {
        TeleportRequestManager.markTeleported(requester, T0);

        long early = TeleportRequestManager.cooldownRemainingMillis(requester, T0, COOLDOWN_SECONDS);
        long later =
                TeleportRequestManager.cooldownRemainingMillis(
                        requester, T0 + 5_000L, COOLDOWN_SECONDS);

        assertEquals(
                COOLDOWN_SECONDS * 1000L,
                early,
                "at t=0 the remaining cooldown should be the full window");
        assertTrue(later < early, "remaining cooldown must decrease as time advances");
        assertEquals(
                (COOLDOWN_SECONDS * 1000L) - 5_000L,
                later,
                "remaining cooldown after 5s should shrink by exactly 5000ms");
    }

    @Test
    void cooldownRemainingMillis_clampsToZeroPastWindow() {
        TeleportRequestManager.markTeleported(requester, T0);

        long past = T0 + (COOLDOWN_SECONDS * 1000L) + 5_000L;

        assertEquals(
                0L,
                TeleportRequestManager.cooldownRemainingMillis(requester, past, COOLDOWN_SECONDS),
                "cooldown remaining must clamp to 0 past the window");
    }

    @Test
    void cooldownRemainingMillis_zeroWhenNoHistory() {
        assertEquals(
                0L,
                TeleportRequestManager.cooldownRemainingMillis(requester, T0, COOLDOWN_SECONDS),
                "cooldown remaining for a player with no history must be 0");
    }

    @Test
    void clearPlayer_removesEntryWherePlayerIsTarget() {
        TeleportRequestManager.request(target, requester, T0);

        TeleportRequestManager.clearPlayer(target);

        assertNull(
                TeleportRequestManager.peek(target),
                "clearPlayer(target) must remove the pending request keyed by target");
    }

    @Test
    void clearPlayer_removesEntriesWherePlayerIsRequester() {
        // The requester has a request outstanding against `target` AND against `other`.
        TeleportRequestManager.request(target, requester, T0);
        TeleportRequestManager.request(other, requester, T0);

        TeleportRequestManager.clearPlayer(requester);

        assertNull(
                TeleportRequestManager.peek(target),
                "clearPlayer(requester) must remove requests where they are the requester (target key)");
        assertNull(
                TeleportRequestManager.peek(other),
                "clearPlayer(requester) must remove requests where they are the requester (other key)");
    }

    @Test
    void clearPlayer_removesCooldownEntry() {
        TeleportRequestManager.markTeleported(requester, T0);
        assertTrue(TeleportRequestManager.isOnCooldown(requester, T0, COOLDOWN_SECONDS));

        TeleportRequestManager.clearPlayer(requester);

        assertFalse(
                TeleportRequestManager.isOnCooldown(requester, T0, COOLDOWN_SECONDS),
                "clearPlayer must drop the player's cooldown entry");
    }

    @Test
    void newerRequest_replacesOlderForSameTarget() {
        UUID firstRequester = UUID.randomUUID();
        UUID secondRequester = UUID.randomUUID();
        // Ensure clean state for the ad-hoc UUIDs used in this test.
        TeleportRequestManager.clearPlayer(firstRequester);
        TeleportRequestManager.clearPlayer(secondRequester);

        TeleportRequestManager.request(target, firstRequester, T0);
        TeleportRequestManager.request(target, secondRequester, T0 + 5_000L);

        Request req = TeleportRequestManager.peek(target);
        assertNotNull(req);
        assertEquals(
                secondRequester,
                req.requesterID(),
                "the newer request must replace the older one for the same target");
        assertEquals(T0 + 5_000L, req.createdAtMillis(), "createdAt must reflect the newer request");
    }
}
