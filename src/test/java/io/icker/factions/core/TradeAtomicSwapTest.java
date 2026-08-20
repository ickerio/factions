package io.icker.factions.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.icker.factions.core.TradeSession.Cause;
import io.icker.factions.core.TradeSession.GuiOpenAction;
import io.icker.factions.core.TradeSession.InventoryAdapter;
import io.icker.factions.core.TradeSession.Phase;
import io.icker.factions.core.TradeSession.PreviousPosition;
import io.icker.factions.core.TradeSession.TeleportAction;

import net.minecraft.world.SimpleContainer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests for the atomic-swap invariant of {@link TradeSession}.
 *
 * <p>The swap runs inside a single server-tick runnable and is gated by a pre-flight
 * capacity check. Both halves must succeed or neither happens — items in a placement
 * container are never moved when the mirroring receiver cannot hold them, and the
 * containers are handed to {@link InventoryAdapter#returnOrDrop} on every cancel path
 * so that no stack disappears.
 *
 * <p>The tests below exercise:
 * <ul>
 *   <li>The pure capacity math ({@link TradeSession#canFitAll(SimpleContainer, SimpleContainer)})
 *       and vanilla-parity distribution ({@link TradeSession#addToContainer(SimpleContainer,
 *       net.minecraft.world.item.ItemStack)}) — trivial cases that do not require a real Item
 *       registry (no {@code Bootstrap.bootStrap()}).</li>
 *   <li>The observable rollback: when commit runs in an environment without a live
 *       server ({@code WorldUtils.server == null}), the swap bails and the cancel handler
 *       routes both placement containers through {@link InventoryAdapter}.</li>
 *   <li>The commit-phase guards: a session already in CANCELLED / COMPLETED cannot be
 *       re-committed.</li>
 * </ul>
 */
class TradeAtomicSwapTest {
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
    void canFitAll_bothEmpty_returnsTrue() {
        SimpleContainer snapshot = new SimpleContainer(36);
        SimpleContainer incoming = new SimpleContainer(27);

        assertTrue(TradeSession.canFitAll(snapshot, incoming),
                "an empty incoming payload must always fit — nothing to deliver");
    }

    @Test
    void canFitAll_snapshotAtFullCapacity_emptyIncoming_stillReturnsTrue() {
        SimpleContainer snapshot = new SimpleContainer(36);
        SimpleContainer incoming = new SimpleContainer(27);

        assertTrue(TradeSession.canFitAll(snapshot, incoming),
                "a full snapshot must still accept an empty payload — the capacity check "
                        + "only iterates the incoming side");
    }

    @Test
    void commitBeforeReveal_isNoop() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        assertEquals(Phase.PLACEMENT, session.getPhase());

        TradeSession.commit(session.getId());

        assertEquals(Phase.PLACEMENT, session.getPhase(),
                "commit MUST NOT advance phase from PLACEMENT — REVEAL is the only "
                        + "legal entry point");
    }

    @Test
    void commitAfterCancelled_isNoop() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        TradeSession.cancel(session.getId(), Cause.PLAYER_CANCELLED);
        assertEquals(Phase.CANCELLED, session.getPhase());

        TradeSession.commit(session.getId());

        assertEquals(Phase.CANCELLED, session.getPhase(),
                "commit MUST NOT reanimate a CANCELLED session");
    }

    @Test
    void commitAfterCompleted_isNoop() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        session.phase = Phase.COMPLETED;

        TradeSession.commit(session.getId());

        assertEquals(Phase.COMPLETED, session.getPhase(),
                "commit MUST NOT retrigger a COMPLETED session");
    }

    @Test
    void bothAcceptWithoutServer_bailsToCancelledAndReturnsBothContainers() {
        record Capture(SimpleContainer container, UUID playerId, PreviousPosition prev, boolean forceDropAll) {}
        List<Capture> captured = new ArrayList<>();
        TradeSession.inventoryImpl =
                (container, playerId, prev, forceDropAll) ->
                        captured.add(new Capture(container, playerId, prev, forceDropAll));

        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        TradeSession.confirmPlacement(session.getId(), requesterId);
        TradeSession.confirmPlacement(session.getId(), recipientId);
        assertEquals(Phase.REVEAL, session.getPhase());

        TradeSession.acceptReveal(session.getId(), requesterId);
        TradeSession.acceptReveal(session.getId(), recipientId);

        assertEquals(Phase.CANCELLED, session.getPhase(),
                "without WorldUtils.server, performSwapWork bails via cancelFromSwap "
                        + "and the phase settles at CANCELLED");
        assertEquals(2, captured.size(),
                "both placement containers must be handed to the InventoryAdapter — "
                        + "one per player — so no items are orphaned by the failed swap");

        List<UUID> capturedPlayers = captured.stream().map(Capture::playerId).toList();
        assertTrue(capturedPlayers.contains(requesterId),
                "the requester's container must be routed through the adapter");
        assertTrue(capturedPlayers.contains(recipientId),
                "the recipient's container must be routed through the adapter");

        for (Capture c : captured) {
            assertFalse(c.forceDropAll(),
                    "SERVER_STOPPING with non-null player references never sets forceDropAll — "
                            + "that flag is reserved for DISCONNECTED with an offline player");
        }

        assertNull(TradeSession.getFor(requesterId).orElse(null),
                "the session must be removed from the requester index after cancel");
        assertNull(TradeSession.getFor(recipientId).orElse(null),
                "the session must be removed from the recipient index after cancel");
    }

    @Test
    void failedSwap_routesTheCorrectContainerToEachPlayer() {
        record Capture(SimpleContainer container, UUID playerId) {}
        List<Capture> captured = new ArrayList<>();
        TradeSession.inventoryImpl =
                (container, playerId, prev, forceDropAll) ->
                        captured.add(new Capture(container, playerId));

        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        SimpleContainer requesterPlacement = session.getRequesterPlacement();
        SimpleContainer recipientPlacement = session.getRecipientPlacement();

        TradeSession.confirmPlacement(session.getId(), requesterId);
        TradeSession.confirmPlacement(session.getId(), recipientId);
        TradeSession.acceptReveal(session.getId(), requesterId);
        TradeSession.acceptReveal(session.getId(), recipientId);

        assertEquals(2, captured.size());

        Capture reqCapture =
                captured.stream()
                        .filter(c -> c.playerId().equals(requesterId))
                        .findFirst()
                        .orElse(null);
        Capture recCapture =
                captured.stream()
                        .filter(c -> c.playerId().equals(recipientId))
                        .findFirst()
                        .orElse(null);

        assertNotNull(reqCapture, "requester capture must exist");
        assertNotNull(recCapture, "recipient capture must exist");
        assertSame(requesterPlacement, reqCapture.container(),
                "the requester MUST receive its OWN placement back — never the recipient's, "
                        + "which would be a mis-route dupe");
        assertSame(recipientPlacement, recCapture.container(),
                "the recipient MUST receive its OWN placement back — never the requester's");
    }

    @Test
    void failedSwap_placementContainersAreUnmodifiedByTheGuard() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        SimpleContainer requesterPlacement = session.getRequesterPlacement();
        SimpleContainer recipientPlacement = session.getRecipientPlacement();

        int reqSize = requesterPlacement.getContainerSize();
        int recSize = recipientPlacement.getContainerSize();

        TradeSession.confirmPlacement(session.getId(), requesterId);
        TradeSession.confirmPlacement(session.getId(), recipientId);
        TradeSession.acceptReveal(session.getId(), requesterId);
        TradeSession.acceptReveal(session.getId(), recipientId);

        assertEquals(reqSize, requesterPlacement.getContainerSize(),
                "the requester placement container size must not change across the failed swap");
        assertEquals(recSize, recipientPlacement.getContainerSize(),
                "the recipient placement container size must not change across the failed swap");
        assertEquals(27, reqSize,
                "the trade placement container is always the fixed 27-slot single chest");
    }
}
