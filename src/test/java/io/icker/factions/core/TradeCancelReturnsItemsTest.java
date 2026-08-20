package io.icker.factions.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Unit tests for the cancel-path item-return contract of {@link TradeSession}.
 *
 * <p>The rule this suite pins is: for every non-server cancel Cause, the
 * {@link InventoryAdapter} is invoked once per player with the correct placement
 * container and the correct {@code forceDropAll} flag. {@code forceDropAll} is
 * ONLY set for {@link Cause#DISCONNECTED} + an offline player — every other Cause
 * MUST route through the "return to inventory, drop overflow" path so that a live
 * player never loses items to the ground.
 *
 * <p>{@link TradeSession#cancel} short-circuits when {@link Phase#SWAPPING},
 * {@link Phase#CANCELLED}, or {@link Phase#COMPLETED} is already set, so all tests
 * start from PLACEMENT and rely on the tests in {@code TradeSessionStateMachineTest}
 * to prove those guards.
 */
class TradeCancelReturnsItemsTest {
    private UUID requesterId;
    private UUID recipientId;

    private TeleportAction savedTeleport;
    private GuiOpenAction savedGuiOpen;
    private InventoryAdapter savedInventory;

    private record Capture(
            SimpleContainer container,
            UUID playerId,
            PreviousPosition prev,
            boolean forceDropAll) {}

    private final List<Capture> captured = new ArrayList<>();

    @BeforeEach
    void resetAndOverrideSeams() {
        requesterId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        captured.clear();

        savedTeleport = TradeSession.teleportImpl;
        savedGuiOpen = TradeSession.guiOpenImpl;
        savedInventory = TradeSession.inventoryImpl;

        TradeSession.teleportImpl = (id, lvl, x, y, z, yaw, pitch) -> {};
        TradeSession.guiOpenImpl = (id, phase) -> {};
        TradeSession.inventoryImpl =
                (container, playerId, prev, forceDropAll) ->
                        captured.add(new Capture(container, playerId, prev, forceDropAll));
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
    void cancelPlayerCancelled_invokesAdapterOncePerPlayerWithForceDropAllFalse() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.PLAYER_CANCELLED);

        assertEquals(2, captured.size(),
                "PLAYER_CANCELLED must invoke the adapter once per player");
        for (Capture c : captured) {
            assertFalse(c.forceDropAll(),
                    "PLAYER_CANCELLED must NEVER set forceDropAll — live players return items");
        }
    }

    @Test
    void cancelDeclined_invokesAdapterWithForceDropAllFalse() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.DECLINED);

        assertEquals(2, captured.size());
        for (Capture c : captured) {
            assertFalse(c.forceDropAll(),
                    "DECLINED must return items, not drop them — the players are online");
        }
    }

    @Test
    void cancelInventoryFull_invokesAdapterWithForceDropAllFalse() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.INVENTORY_FULL);

        assertEquals(2, captured.size());
        for (Capture c : captured) {
            assertFalse(c.forceDropAll(),
                    "INVENTORY_FULL must return items — a full inventory is not a disconnect");
        }
    }

    @Test
    void cancelGuiClosed_invokesAdapterWithForceDropAllFalse() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.GUI_CLOSED);

        assertEquals(2, captured.size());
        for (Capture c : captured) {
            assertFalse(c.forceDropAll(),
                    "GUI_CLOSED must return items — closing the screen does not mean the "
                            + "player left");
        }
    }

    @Test
    void cancelFactionDisbanded_invokesAdapterWithForceDropAllFalse() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.FACTION_DISBANDED);

        assertEquals(2, captured.size());
        for (Capture c : captured) {
            assertFalse(c.forceDropAll(),
                    "FACTION_DISBANDED must return items — the players did not leave the server");
        }
    }

    @Test
    void cancelServerStopping_invokesAdapterWithForceDropAllFalse() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.SERVER_STOPPING);

        assertEquals(2, captured.size());
        for (Capture c : captured) {
            assertFalse(c.forceDropAll(),
                    "SERVER_STOPPING must not force-drop — the flag is scoped to DISCONNECTED "
                            + "with a null player reference");
        }
    }

    @Test
    void cancelDisconnected_offlinePlayers_setsForceDropAllTrue() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);

        TradeSession.cancel(session.getId(), Cause.DISCONNECTED);

        assertEquals(2, captured.size());
        for (Capture c : captured) {
            assertTrue(c.forceDropAll(),
                    "DISCONNECTED with no live server (players offline) must set forceDropAll — "
                            + "there is no inventory to return items to");
        }
    }

    @Test
    void cancel_routesTheCorrectContainerToEachPlayerId() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        SimpleContainer requesterPlacement = session.getRequesterPlacement();
        SimpleContainer recipientPlacement = session.getRecipientPlacement();

        TradeSession.cancel(session.getId(), Cause.PLAYER_CANCELLED);

        assertEquals(2, captured.size());
        Capture forRequester =
                captured.stream()
                        .filter(c -> c.playerId().equals(requesterId))
                        .findFirst()
                        .orElseThrow();
        Capture forRecipient =
                captured.stream()
                        .filter(c -> c.playerId().equals(recipientId))
                        .findFirst()
                        .orElseThrow();

        assertSame(requesterPlacement, forRequester.container(),
                "the requester MUST receive its OWN placement container back — "
                        + "cross-wired containers would be a dupe/loss bug");
        assertSame(recipientPlacement, forRecipient.container(),
                "the recipient MUST receive its OWN placement container back");
    }

    @Test
    void cancel_passesTheStoredPreviousPositionToTheAdapter() {
        TradeSession session = TradeSession.beginForTest(requesterId, recipientId);
        PreviousPosition expectedRequesterPrev = session.prevPositionFor(requesterId);
        PreviousPosition expectedRecipientPrev = session.prevPositionFor(recipientId);

        TradeSession.cancel(session.getId(), Cause.PLAYER_CANCELLED);

        Capture forRequester =
                captured.stream()
                        .filter(c -> c.playerId().equals(requesterId))
                        .findFirst()
                        .orElseThrow();
        Capture forRecipient =
                captured.stream()
                        .filter(c -> c.playerId().equals(recipientId))
                        .findFirst()
                        .orElseThrow();

        assertSame(expectedRequesterPrev, forRequester.prev(),
                "the adapter must receive the pre-session position for overflow drop");
        assertSame(expectedRecipientPrev, forRecipient.prev(),
                "the adapter must receive the pre-session position for overflow drop");
    }
}
