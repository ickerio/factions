package io.icker.factions.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;

import io.icker.factions.core.TradeSession;
import io.icker.factions.core.TradeSession.Cause;
import io.icker.factions.core.TradeSession.Phase;
import io.icker.factions.util.WorldUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Phase 1 of the /f trade flow: each player privately places items in their own 27-slot chest.
 *
 * <p>Slots 0-17 (rows 1-2) are redirected via {@link Slot} onto the session's per-player {@link
 * SimpleContainer}, so vanilla handles all click/drag/quick-move interactions natively and mutates
 * the container directly. Row 3 (slots 18-26) is the control row: slot 22 is the slimeball CONFIRM,
 * slot 24 is the magma-cream CANCEL, and the remaining row-3 slots hold gray glass-pane filler with
 * no callback. Because filler slots are set via {@link GuiElementBuilder} (no backing container),
 * sgui blocks item movement into them by default — no {@code onAnyClick} override needed.
 */
public class TradePlacementGui extends SimpleGui {

    public static final int SLOT_CONFIRM = 22;
    public static final int SLOT_CANCEL = 24;
    public static final int PLACEMENT_SLOT_COUNT = 18;

    private static final int[] FILLER_SLOTS = {18, 19, 20, 21, 23, 25, 26};

    private final UUID sessionId;
    private boolean confirmed = false;

    public TradePlacementGui(ServerPlayer player, UUID sessionId) {
        super(MenuType.GENERIC_9x3, player, false);
        this.sessionId = sessionId;

        TradeSession session =
                TradeSession.getFor(player.getUUID())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No active trade session for player "
                                                        + player.getUUID()));

        if (session.getPhase() != Phase.PLACEMENT) {
            throw new IllegalStateException(
                    "Trade session " + sessionId + " is not in PLACEMENT phase: "
                            + session.getPhase());
        }

        SimpleContainer placement = session.ownPlacementFor(player);

        this.setTitle(Component.translatable("factions.gui.trade.placement.title"));

        for (int i = 0; i < PLACEMENT_SLOT_COUNT; i++) {
            this.setSlot(i, new Slot(placement, i, 0, 0));
        }

        for (int slot : FILLER_SLOTS) {
            this.setSlot(
                    slot,
                    new GuiElementBuilder(Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY))
                            .hideTooltip());
        }

        this.setSlot(
                SLOT_CONFIRM,
                new GuiElementBuilder(Items.SLIME_BALL)
                        .setName(
                                Component.translatable("factions.gui.trade.placement.confirm"))
                        .setCallback(
                                (index, clickType, actionType, gui) -> {
                                    if (confirmed) return;
                                    confirmed = true;
                                    TradeSession.confirmPlacement(sessionId, player.getUUID());
                                    TradeSession.getFor(player.getUUID())
                                            .ifPresent(
                                                    s -> {
                                                        if (s.getPhase() != Phase.REVEAL) return;
                                                        MinecraftServer srv = WorldUtils.server;
                                                        if (srv == null) return;
                                                        ServerPlayer req =
                                                                srv.getPlayerList()
                                                                        .getPlayer(
                                                                                s.getRequesterId());
                                                        ServerPlayer rec =
                                                                srv.getPlayerList()
                                                                        .getPlayer(
                                                                                s.getRecipientId());
                                                        // TradeRevealGui does NOT auto-open —
                                                        // caller must invoke .open().
                                                        if (req != null) {
                                                            new TradeRevealGui(req, s.getId())
                                                                    .open();
                                                        }
                                                        if (rec != null) {
                                                            new TradeRevealGui(rec, s.getId())
                                                                    .open();
                                                        }
                                                    });
                                }));

        this.setSlot(
                SLOT_CANCEL,
                new GuiElementBuilder(Items.MAGMA_CREAM)
                        .setName(
                                Component.translatable("factions.gui.trade.placement.cancel"))
                        .setCallback(
                                (index, clickType, actionType, gui) -> {
                                    TradeSession.cancel(sessionId, Cause.PLAYER_CANCELLED);
                                }));

        session.setGuiFor(player.getUUID(), this);

        this.open();
    }

    @Override
    public void onPlayerClose(boolean immediate) {
        TradeSession.getFor(player.getUUID())
                .ifPresent(
                        session -> {
                            if (session.getPhase() == Phase.PLACEMENT && !confirmed) {
                                TradeSession.cancel(sessionId, Cause.GUI_CLOSED);
                            }
                        });
        super.onPlayerClose(immediate);
    }
}
