package io.icker.factions.ui;

import eu.pb4.sgui.api.ClickType;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Phase 2 of the trade flow: both players see each other's placed items (read-only) and choose
 * ACCEPT (slimeball), NEGOTIATE (ender pearl = back to Phase 1), or DECLINE (magma cream).
 *
 * <p>Anti-dupe invariant: NO slot in this GUI is linked to a live container. Every slot is a
 * static {@link GuiElementBuilder} copy of an {@link ItemStack}. All clicks except the three
 * action buttons are blocked at the outer {@link #onAnyClick} gate, and every per-slot callback
 * is an empty lambda — a click can neither reach vanilla item movement nor a live redirect.
 */
public class TradeRevealGui extends SimpleGui {

    public static final int SLOT_ACCEPT = 20;
    public static final int SLOT_NEGOTIATE = 22;
    public static final int SLOT_DECLINE = 24;

    private final UUID sessionId;
    private boolean accepted = false;

    public TradeRevealGui(ServerPlayer player, UUID sessionId) {
        super(MenuType.GENERIC_9x3, player, false);
        this.sessionId = sessionId;

        this.setTitle(Component.translatable("factions.gui.trade.reveal.title"));

        TradeSession session =
                TradeSession.getFor(player.getUUID())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No active trade session for player "
                                                + player.getUUID()));

        // Slots 0-17: static copies of the OTHER player's placement items (read-only).
        // .copy() ensures the GUI element cannot alias the live SimpleContainer's stacks.
        SimpleContainer other = session.otherPlacementFor(player);
        for (int i = 0; i < 18; i++) {
            ItemStack stack = other.getItem(i).copy();
            if (stack.isEmpty()) {
                this.setSlot(
                        i,
                        new GuiElementBuilder(Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY))
                                .hideTooltip()
                                .setCallback((index, clickType, input, gui) -> {}));
            } else {
                this.setSlot(
                        i,
                        GuiElementBuilder.from(stack)
                                .setCallback((index, clickType, input, gui) -> {}));
            }
        }

        // Filler slots in the button row (slots 18, 19, 21, 23, 25, 26).
        int[] fillerSlots = {18, 19, 21, 23, 25, 26};
        for (int slot : fillerSlots) {
            this.setSlot(
                    slot,
                    new GuiElementBuilder(Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY))
                            .setName(Component.translatable("factions.trade.filler"))
                            .hideTooltip()
                            .setCallback((index, clickType, input, gui) -> {}));
        }

        // ACCEPT (slot 20 = slimeball). If both players accept, TradeSession transitions to
        // SWAPPING and invokes commit() (Task 10 fills the atomic-swap body).
        this.setSlot(
                SLOT_ACCEPT,
                new GuiElementBuilder(Items.SLIME_BALL)
                        .setName(Component.translatable("factions.trade.accept"))
                        .setCallback(
                                (index, clickType, input, gui) -> {
                                    if (accepted) return;
                                    accepted = true;
                                    TradeSession.acceptReveal(sessionId, player.getUUID());
                                }));

        // NEGOTIATE (slot 22 = ender pearl). Resets both confirm/accept flags and moves phase
        // back to PLACEMENT — items in the placement containers are untouched. Task 9 wires the
        // "after-renegotiate open-TradePlacementGui" flow for both players.
        this.setSlot(
                SLOT_NEGOTIATE,
                new GuiElementBuilder(Items.ENDER_PEARL)
                        .setName(Component.translatable("factions.trade.negotiate"))
                        .setCallback(
                                (index, clickType, input, gui) -> {
                                    TradeSession.renegotiate(sessionId);
                                    TradeSession.getFor(player.getUUID())
                                            .ifPresent(
                                                    s -> {
                                                        if (s.getPhase() != Phase.PLACEMENT) return;
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
                                                        // TradePlacementGui auto-opens in its
                                                        // constructor — do NOT call .open().
                                                        if (req != null) {
                                                            new TradePlacementGui(req, s.getId());
                                                        }
                                                        if (rec != null) {
                                                            new TradePlacementGui(rec, s.getId());
                                                        }
                                                    });
                                }));

        // DECLINE (slot 24 = magma cream). Cancels with Cause.DECLINED — items are returned to
        // both players' inventories via TradeSession.cancel's inventoryImpl seam.
        this.setSlot(
                SLOT_DECLINE,
                new GuiElementBuilder(Items.MAGMA_CREAM)
                        .setName(Component.translatable("factions.trade.decline"))
                        .setCallback(
                                (index, clickType, input, gui) -> {
                                    TradeSession.cancel(sessionId, Cause.DECLINED);
                                }));

        // Register this GUI with the session so TradeSession.cancel() / commit() can close it.
        session.setGuiFor(player.getUUID(), this);
    }

    /**
     * Belt-and-suspenders: block ALL non-button clicks BEFORE they reach per-slot callbacks or
     * vanilla item-movement handlers. The per-slot empty callbacks are the primary layer; this
     * outer gate is the backup.
     *
     * <p>Returning {@code true} for the three action buttons lets the click flow through to the
     * per-slot {@link GuiElementBuilder} callback registered above. Returning {@code false} for
     * every other slot cancels the click entirely — no drag, shift-click, double-click, hotbar
     * swap, or drop-key can move an item.
     */
    @Override
    public boolean onAnyClick(int index, ClickType type, ContainerInput input) {
        if (index == SLOT_ACCEPT || index == SLOT_NEGOTIATE || index == SLOT_DECLINE) {
            return super.onAnyClick(index, type, input);
        }
        return false;
    }

    /**
     * If the player closes the GUI (ESC / walk-away / disconnect) without accepting, cancel the
     * session. The phase guard prevents double-cancel when a programmatic close (from
     * {@link TradeSession#cancel} after DECLINE, or a phase transition to PLACEMENT via
     * {@link TradeSession#renegotiate}, or to SWAPPING via {@link TradeSession#acceptReveal})
     * triggered the close. Mirrors {@code TradePlacementGui#onPlayerClose} for consistency.
     */
    @Override
    public void onPlayerClose(boolean immediate) {
        TradeSession.getFor(player.getUUID())
                .ifPresent(
                        session -> {
                            if (session.getPhase() == Phase.REVEAL && !accepted) {
                                TradeSession.cancel(sessionId, Cause.GUI_CLOSED);
                            }
                        });
        super.onPlayerClose(immediate);
    }
}
