package io.icker.factions.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.icker.factions.core.TradeSession.Cause;
import io.icker.factions.core.TradeSession.GuiOpenAction;
import io.icker.factions.core.TradeSession.InventoryAdapter;
import io.icker.factions.core.TradeSession.Phase;
import io.icker.factions.core.TradeSession.TeleportAction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Unit tests for {@link TradeSession}'s phase transitions.
 *
 * <p>{@code TradeSession} maintains a static {@link java.util.concurrent.ConcurrentHashMap} of
 * live sessions plus a per-player index. Every test allocates fresh {@link UUID}s in
 * {@link #resetAndOverrideSeams()} and cancels any lingering session in {@link #cleanupSessions()}
 * so that state cannot leak between tests. The seams {@link TeleportAction},
 * {@link GuiOpenAction} and {@link InventoryAdapter} are also captured and restored to guarantee
 * one test's stub cannot bleed into another.
 */
class TradeSessionStateMachineTest {
    private UUID requesterId;
    private UUID recipientId;

    private TeleportAction savedTeleport;
    private GuiOpenAction savedGuiOpen;
    private InventoryAdapter savedInventory;

    @BeforeEach
    void resetAndOverrideSeams() {
        requesterId = UUID.randomUUID();
        recipientId = UUID.randomUUID();

        savedTeleport = TradeSession.teleportImpl;
        savedGuiOpen = TradeSession.guiOpenImpl;
        savedInventory = TradeSession.inventoryImpl;

        TradeSession.teleportImpl = (id, lvl, x, y, z, yaw, pitch) -> {};
        TradeSession.guiOpenImpl = (id, phase) -> {};
        TradeSession.inventoryImpl = (container, playerId, prev, forceDropAll) -> {};
    }

    @AfterEach
    void cleanupSessions() {
        TradeSession.getFor(requesterId)
                .ifPresent(s -> TradeSession.cancel(s.getId(), Cause.SERVER_STOPPING));
        TradeSession.getFor(recipientId)
                .ifPresent(s -> TradeSession.cancel(s.getId(), Cause.SERVER_STOPPING));

        TradeSession.teleportImpl = savedTeleport;
        TradeSession.guiOpenImpl = savedGuiOpen;
        TradeSession.inventoryImpl = savedInventory;
    }

    @Test
    void beginSession_phaseIsPlacement() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        assertNotNull(session, "beginForTest must register a session");
        assertEquals(Phase.PLACEMENT, session.getPhase(),
                "a fresh session must start in PLACEMENT");
        assertEquals(session,
                TradeSession.getFor(requesterId).orElse(null),
                "the requester must be indexed to the new session");
        assertEquals(session,
                TradeSession.getFor(recipientId).orElse(null),
                "the recipient must be indexed to the new session");
    }

    @Test
    void confirmPlacement_bothConfirm_phaseBecomesReveal() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.confirmPlacement(session.getId(), requesterId);
        assertEquals(Phase.PLACEMENT, session.getPhase(),
                "one confirm must not advance out of PLACEMENT");

        TradeSession.confirmPlacement(session.getId(), recipientId);
        assertEquals(Phase.REVEAL, session.getPhase(),
                "both confirms must advance to REVEAL");
    }

    @Test
    void confirmPlacement_oneConfirm_phaseStaysPlacement() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.confirmPlacement(session.getId(), requesterId);

        assertEquals(Phase.PLACEMENT, session.getPhase(),
                "only requester confirmed — must still be PLACEMENT");
        assertTrue(session.requesterConfirmed,
                "requester confirm flag must be flipped");
        assertFalse(session.recipientConfirmed,
                "recipient confirm flag must remain unset");
    }

    @Test
    void renegotiate_fromReveal_phaseBecomesPlacement() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        TradeSession.confirmPlacement(session.getId(), requesterId);
        TradeSession.confirmPlacement(session.getId(), recipientId);
        assertEquals(Phase.REVEAL, session.getPhase());

        TradeSession.renegotiate(session.getId());

        assertEquals(Phase.PLACEMENT, session.getPhase(),
                "renegotiate must return to PLACEMENT");
        assertFalse(session.requesterConfirmed,
                "renegotiate must clear the requester confirm flag");
        assertFalse(session.recipientConfirmed,
                "renegotiate must clear the recipient confirm flag");
        assertFalse(session.requesterAccepted,
                "renegotiate must clear the requester accept flag");
        assertFalse(session.recipientAccepted,
                "renegotiate must clear the recipient accept flag");
    }

    @Test
    void acceptReveal_bothAccept_transitionsOutOfReveal() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        TradeSession.confirmPlacement(session.getId(), requesterId);
        TradeSession.confirmPlacement(session.getId(), recipientId);
        assertEquals(Phase.REVEAL, session.getPhase());

        TradeSession.acceptReveal(session.getId(), requesterId);
        assertEquals(Phase.REVEAL, session.getPhase(),
                "one accept must not advance out of REVEAL");
        assertTrue(session.requesterAccepted);
        assertFalse(session.recipientAccepted);

        TradeSession.acceptReveal(session.getId(), recipientId);
        assertTrue(session.recipientAccepted, "both accept flags must be set");

        assertNotEquals(Phase.REVEAL, session.getPhase(),
                "both accepts must advance out of REVEAL — commit was triggered");
    }

    @Test
    void cancelFromPlacement_phaseBecomesCancelled() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.PLAYER_CANCELLED);

        assertEquals(Phase.CANCELLED, session.getPhase(),
                "cancel from PLACEMENT must transition to CANCELLED");
    }

    @Test
    void doubleCancelIsNoop() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.PLAYER_CANCELLED);
        assertEquals(Phase.CANCELLED, session.getPhase());

        TradeSession.cancel(session.getId(), Cause.DECLINED);

        assertEquals(Phase.CANCELLED, session.getPhase(),
                "second cancel must NOT change the CANCELLED phase");
    }

    @Test
    void cancelAfterCompletedIsNoop() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        session.phase = Phase.COMPLETED;

        TradeSession.cancel(session.getId(), Cause.PLAYER_CANCELLED);

        assertEquals(Phase.COMPLETED, session.getPhase(),
                "cancel MUST NOT overwrite a COMPLETED phase");
    }
}
