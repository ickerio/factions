package io.icker.factions.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the slot constants exposed by {@link TradePlacementGui} and
 * {@link TradeRevealGui}.
 *
 * <p>The GUI classes extend {@code eu.pb4.sgui.api.gui.SimpleGui} and require a live
 * {@link net.minecraft.server.level.ServerPlayer} to instantiate — that is covered by
 * F3 QA in-game. This suite pins the {@code public static final int} constants that the
 * event listener and control-slot handlers dispatch on. Per JVM §5.5 constant primitive
 * field access does NOT trigger class initialisation, so this test never touches sgui.
 *
 * <p>Rationale for pinning each constant: the placement GUI ships with slots 0-17 as
 * the two-row deposit grid (slot 22 CONFIRM sits in the row-3 centre, slot 24 CANCEL two
 * slots right of it). The reveal GUI shrinks the deposit block to zero and moves the
 * three action buttons to slots 20 / 22 / 24. Silently drifting either constant off by
 * one flips a filler pane into a click target that mutates state — a duplication or
 * grief vector.
 */
class TradeGuiSlotsTest {

    @Test
    void placementGui_confirmSlot_is22() {
        assertEquals(22, TradePlacementGui.SLOT_CONFIRM,
                "SLOT_CONFIRM must remain 22 — centre of row 3, matching the slimeball icon");
    }

    @Test
    void placementGui_cancelSlot_is24() {
        assertEquals(24, TradePlacementGui.SLOT_CANCEL,
                "SLOT_CANCEL must remain 24 — two slots right of confirm, matching the magma cream icon");
    }

    @Test
    void placementGui_placementSlotCount_is18() {
        assertEquals(18, TradePlacementGui.PLACEMENT_SLOT_COUNT,
                "the placement grid is exactly the first two rows (slots 0-17) of a 9x3 chest");
    }

    @Test
    void placementGui_confirmAndCancelSlotsAreDistinct() {
        assertNotEquals(TradePlacementGui.SLOT_CONFIRM, TradePlacementGui.SLOT_CANCEL,
                "confirm and cancel must NEVER collide — a single button doing both is a "
                        + "duplication vector");
    }

    @Test
    void placementGui_controlSlotsSitOutsideThePlacementGrid() {
        assertEquals(true, TradePlacementGui.SLOT_CONFIRM >= TradePlacementGui.PLACEMENT_SLOT_COUNT,
                "SLOT_CONFIRM must land in row 3, past the placement grid — otherwise a "
                        + "click on the button would also mutate the underlying container");
        assertEquals(true, TradePlacementGui.SLOT_CANCEL >= TradePlacementGui.PLACEMENT_SLOT_COUNT,
                "SLOT_CANCEL must land in row 3, past the placement grid");
    }

    @Test
    void revealGui_acceptSlot_is20() {
        assertEquals(20, TradeRevealGui.SLOT_ACCEPT,
                "SLOT_ACCEPT must remain 20 — left of centre in row 3, matching the emerald icon");
    }

    @Test
    void revealGui_negotiateSlot_is22() {
        assertEquals(22, TradeRevealGui.SLOT_NEGOTIATE,
                "SLOT_NEGOTIATE must remain 22 — centre of row 3, matching the anvil icon");
    }

    @Test
    void revealGui_declineSlot_is24() {
        assertEquals(24, TradeRevealGui.SLOT_DECLINE,
                "SLOT_DECLINE must remain 24 — right of centre in row 3, matching the magma cream icon");
    }

    @Test
    void revealGui_threeActionSlotsAreDistinct() {
        assertNotEquals(TradeRevealGui.SLOT_ACCEPT, TradeRevealGui.SLOT_NEGOTIATE,
                "accept and negotiate must NEVER collide");
        assertNotEquals(TradeRevealGui.SLOT_ACCEPT, TradeRevealGui.SLOT_DECLINE,
                "accept and decline must NEVER collide");
        assertNotEquals(TradeRevealGui.SLOT_NEGOTIATE, TradeRevealGui.SLOT_DECLINE,
                "negotiate and decline must NEVER collide");
    }

    @Test
    void revealGui_actionSlotsAreEvenlySpaced() {
        int accept = TradeRevealGui.SLOT_ACCEPT;
        int negotiate = TradeRevealGui.SLOT_NEGOTIATE;
        int decline = TradeRevealGui.SLOT_DECLINE;

        assertEquals(2, negotiate - accept,
                "accept -> negotiate must be a 2-slot gap so a filler pane separates them");
        assertEquals(2, decline - negotiate,
                "negotiate -> decline must be a 2-slot gap so a filler pane separates them");
    }
}
