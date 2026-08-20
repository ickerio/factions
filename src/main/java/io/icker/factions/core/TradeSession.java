package io.icker.factions.core;

import eu.pb4.sgui.api.gui.SimpleGui;

import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.config.TradeStageConfig;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state machine and registry for the two-phase /f trade GUI system.
 *
 * <p>Not persisted: active sessions are ephemeral and are cancelled on server shutdown via {@link
 * #clearAllForShutdown()}.
 *
 * <p>Task 3 defines the API skeleton. {@link #commit(UUID)} body is filled in by Task 10; event
 * listener registrations in {@link #register()} are filled in by Task 11.
 */
public final class TradeSession {
    /** Lifecycle phase. Transitions are one-way except {@code REVEAL -> PLACEMENT} via renegotiate. */
    public enum Phase {
        PLACEMENT,
        REVEAL,
        SWAPPING,
        CANCELLED,
        COMPLETED
    }

    /** Reason a session was cancelled — used to route item-return logic and actionbar message. */
    public enum Cause {
        PLAYER_CANCELLED,
        DECLINED,
        INVENTORY_FULL,
        DISCONNECTED,
        GUI_CLOSED,
        FACTION_DISBANDED,
        SERVER_STOPPING
    }

    /** Snapshot of a player's location captured at {@link #begin} for return-teleport on cancel. */
    public record PreviousPosition(
            String levelResource, double x, double y, double z, float yaw, float pitch) {}

    /** Seam: teleport a player by UUID. Real impl looks up ServerPlayer + ServerLevel and calls teleportTo. */
    @FunctionalInterface
    public interface TeleportAction {
        void teleport(
                UUID playerId,
                String levelResource,
                double x,
                double y,
                double z,
                float yaw,
                float pitch);
    }

    /** Seam: open the phase-appropriate GUI for a player. Real impl is provided by the GUI layer. */
    @FunctionalInterface
    public interface GuiOpenAction {
        void open(UUID playerId, Phase phase);
    }

    /**
     * Seam: return items from a placement container to a player's inventory, dropping overflow at
     * the player's previous position. If {@code forceDropAll} is true (disconnecting player), drop
     * every stack instead of returning any. Clears the container after processing.
     */
    @FunctionalInterface
    public interface InventoryAdapter {
        void returnOrDrop(
                SimpleContainer container,
                UUID playerId,
                PreviousPosition prev,
                boolean forceDropAll);
    }

    /** Default teleport: look up the online player + world and invoke real MC teleportTo. */
    static TeleportAction teleportImpl =
            (id, lvl, x, y, z, yaw, pitch) -> {
                MinecraftServer server = WorldUtils.server;
                if (server == null) return;
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player == null) return;
                ServerLevel world = WorldUtils.getWorld(lvl);
                if (world == null) return;
                player.teleportTo(world, x, y, z, new HashSet<>(), yaw, pitch, false);
            };

    /** Default GUI-open: no-op. The GUI layer (Task 5/6) wires this to open the correct screen. */
    static GuiOpenAction guiOpenImpl = (id, phase) -> {
        /* caller opens the GUI directly — no default behavior */
    };

    /** Default inventory: online player gets return-with-overflow-drop; offline gets full drop. */
    static InventoryAdapter inventoryImpl =
            (container, playerId, prev, forceDropAll) -> {
                MinecraftServer server = WorldUtils.server;
                ServerPlayer player =
                        server == null ? null : server.getPlayerList().getPlayer(playerId);
                if (forceDropAll || player == null) {
                    dropItems(container, prev);
                } else {
                    returnItems(container, player, prev);
                }
            };

    private static final Map<UUID, TradeSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> BY_PLAYER = new ConcurrentHashMap<>();

    final UUID sessionId;
    final UUID requesterId;
    final UUID recipientId;
    final PreviousPosition requesterPrev;
    final PreviousPosition recipientPrev;
    volatile Phase phase = Phase.PLACEMENT;
    final SimpleContainer requesterPlacement = new SimpleContainer(27);
    final SimpleContainer recipientPlacement = new SimpleContainer(27);
    volatile boolean requesterConfirmed = false;
    volatile boolean recipientConfirmed = false;
    volatile boolean requesterAccepted = false;
    volatile boolean recipientAccepted = false;
    SimpleGui requesterGui = null;
    SimpleGui recipientGui = null;

    private TradeSession(
            UUID sessionId,
            UUID requesterId,
            UUID recipientId,
            PreviousPosition requesterPrev,
            PreviousPosition recipientPrev) {
        this.sessionId = sessionId;
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.requesterPrev = requesterPrev;
        this.recipientPrev = recipientPrev;
    }

    // ---------------------------------------------------------------------- instance accessors

    public UUID getId() {
        return sessionId;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public Phase getPhase() {
        return phase;
    }

    public SimpleContainer getRequesterPlacement() {
        return requesterPlacement;
    }

    public SimpleContainer getRecipientPlacement() {
        return recipientPlacement;
    }

    public SimpleContainer ownPlacementFor(ServerPlayer player) {
        return player.getUUID().equals(requesterId) ? requesterPlacement : recipientPlacement;
    }

    public SimpleContainer otherPlacementFor(ServerPlayer player) {
        return player.getUUID().equals(requesterId) ? recipientPlacement : requesterPlacement;
    }

    public PreviousPosition prevPositionFor(UUID playerId) {
        return playerId.equals(requesterId) ? requesterPrev : recipientPrev;
    }

    /**
     * Register the given player's active GUI so {@link #cancel} can force-close it. Called by the
     * GUI layer during construction; a null-safe no-op if {@code playerId} is not part of this
     * session.
     */
    public void setGuiFor(UUID playerId, SimpleGui gui) {
        if (playerId.equals(requesterId)) {
            this.requesterGui = gui;
        } else if (playerId.equals(recipientId)) {
            this.recipientGui = gui;
        }
    }

    // ---------------------------------------------------------------------- static API

    /**
     * Begin a new session: validates neither player is already in one, captures pre-positions,
     * teleports both to their staging spots, and registers the session.
     *
     * @return the new session, or {@code null} if validation fails or the stage world is unavailable
     */
    public static TradeSession begin(
            ServerPlayer requester, ServerPlayer recipient, TradeStageConfig cfg) {
        if (BY_PLAYER.containsKey(requester.getUUID())
                || BY_PLAYER.containsKey(recipient.getUUID())) {
            return null;
        }

        ServerLevel stageWorld = WorldUtils.getWorld(cfg.LEVEL);
        if (stageWorld == null) return null;

        PreviousPosition requesterPrev = capturePreviousPosition(requester);
        PreviousPosition recipientPrev = capturePreviousPosition(recipient);

        teleportImpl.teleport(
                requester.getUUID(),
                cfg.LEVEL,
                cfg.REQUESTER_X,
                cfg.REQUESTER_Y,
                cfg.REQUESTER_Z,
                cfg.REQUESTER_YAW,
                cfg.REQUESTER_PITCH);
        teleportImpl.teleport(
                recipient.getUUID(),
                cfg.LEVEL,
                cfg.RECIPIENT_X,
                cfg.RECIPIENT_Y,
                cfg.RECIPIENT_Z,
                cfg.RECIPIENT_YAW,
                cfg.RECIPIENT_PITCH);

        UUID id = UUID.randomUUID();
        TradeSession session =
                new TradeSession(
                        id, requester.getUUID(), recipient.getUUID(), requesterPrev, recipientPrev);
        SESSIONS.put(id, session);
        BY_PLAYER.put(requester.getUUID(), id);
        BY_PLAYER.put(recipient.getUUID(), id);
        return session;
    }

    /**
     * Flip the per-player Confirm flag; if both are set, advance to {@link Phase#REVEAL}. Does NOT
     * open the reveal GUI — the caller polls or subscribes.
     */
    public static void confirmPlacement(UUID sessionId, UUID playerId) {
        TradeSession session = SESSIONS.get(sessionId);
        if (session == null || session.phase != Phase.PLACEMENT) return;

        if (playerId.equals(session.requesterId)) {
            session.requesterConfirmed = true;
        } else if (playerId.equals(session.recipientId)) {
            session.recipientConfirmed = true;
        }

        if (session.requesterConfirmed && session.recipientConfirmed) {
            session.phase = Phase.REVEAL;
        }
    }

    /**
     * Flip the per-player Accept flag; if both are set, advance to {@link Phase#SWAPPING} and call
     * {@link #commit(UUID)}.
     */
    public static void acceptReveal(UUID sessionId, UUID playerId) {
        TradeSession session = SESSIONS.get(sessionId);
        if (session == null || session.phase != Phase.REVEAL) return;

        if (playerId.equals(session.requesterId)) {
            session.requesterAccepted = true;
        } else if (playerId.equals(session.recipientId)) {
            session.recipientAccepted = true;
        }

        if (session.requesterAccepted && session.recipientAccepted) {
            session.phase = Phase.SWAPPING;
            commit(sessionId);
        }
    }

    /**
     * Return to Placement without clearing item contents. Both confirm and accept flags are
     * cleared; items in Placement containers stay untouched.
     */
    public static void renegotiate(UUID sessionId) {
        TradeSession session = SESSIONS.get(sessionId);
        if (session == null) return;
        session.requesterConfirmed = false;
        session.recipientConfirmed = false;
        session.requesterAccepted = false;
        session.recipientAccepted = false;
        session.phase = Phase.PLACEMENT;
    }

    /**
     * Idempotently cancel a session: flip phase to CANCELLED, return items (or drop on disconnect),
     * teleport online players back, close GUIs, notify players, and remove the session from the
     * registry. All mutations run on the server main thread.
     */
    public static void cancel(UUID sessionId, Cause cause) {
        TradeSession session = SESSIONS.get(sessionId);
        if (session == null) return;

        // SWAPPING is terminal-in-progress: it blocks external cancels racing commit's
        // scheduled runnable. Internal failure paths inside performSwapWork bypass
        // this guard via cancelFromSwap().
        synchronized (session) {
            if (session.phase == Phase.CANCELLED
                    || session.phase == Phase.COMPLETED
                    || session.phase == Phase.SWAPPING) return;
            session.phase = Phase.CANCELLED;
        }

        runOnMainThread(() -> performCancelWork(session, cause));
    }

    private static void performCancelWork(TradeSession session, Cause cause) {
        MinecraftServer server = WorldUtils.server;
        ServerPlayer requester =
                server == null ? null : server.getPlayerList().getPlayer(session.requesterId);
        ServerPlayer recipient =
                server == null ? null : server.getPlayerList().getPlayer(session.recipientId);

        // Item return: DISCONNECTED drops the offline player's items; others attempt inventory add.
        boolean requesterDropAll = cause == Cause.DISCONNECTED && requester == null;
        boolean recipientDropAll = cause == Cause.DISCONNECTED && recipient == null;
        inventoryImpl.returnOrDrop(
                session.requesterPlacement,
                session.requesterId,
                session.requesterPrev,
                requesterDropAll);
        inventoryImpl.returnOrDrop(
                session.recipientPlacement,
                session.recipientId,
                session.recipientPrev,
                recipientDropAll);

        // Teleport online players back to their pre-session position.
        if (requester != null) {
            teleportImpl.teleport(
                    session.requesterId,
                    session.requesterPrev.levelResource(),
                    session.requesterPrev.x(),
                    session.requesterPrev.y(),
                    session.requesterPrev.z(),
                    session.requesterPrev.yaw(),
                    session.requesterPrev.pitch());
        }
        if (recipient != null) {
            teleportImpl.teleport(
                    session.recipientId,
                    session.recipientPrev.levelResource(),
                    session.recipientPrev.x(),
                    session.recipientPrev.y(),
                    session.recipientPrev.z(),
                    session.recipientPrev.yaw(),
                    session.recipientPrev.pitch());
        }

        // Close any open GUIs.
        if (session.requesterGui != null) session.requesterGui.close();
        if (session.recipientGui != null) session.recipientGui.close();

        // Notify online players via actionbar.
        Component msg = Component.translatable(actionbarKey(cause));
        if (requester != null) new Message(msg.copy()).send(requester, true);
        if (recipient != null) new Message(msg.copy()).send(recipient, true);

        // Deregister.
        SESSIONS.remove(session.sessionId);
        BY_PLAYER.remove(session.requesterId);
        BY_PLAYER.remove(session.recipientId);
    }

    /**
     * Atomic two-player inventory swap. Runs the pre-flight capacity check and the
     * actual swap inside a single {@code server.execute()} runnable so both fire on
     * the same server tick — this eliminates TOCTOU between "we simulated it would
     * fit" and "we mutated". Failure of ANY pre-flight check aborts before touching
     * either placement container, keeping the swap all-or-nothing.
     */
    public static void commit(UUID sessionId) {
        TradeSession session = SESSIONS.get(sessionId);
        if (session == null
                || (session.phase != Phase.REVEAL && session.phase != Phase.SWAPPING)) return;

        // Phase must flip BEFORE scheduling: any external cancel arriving between
        // this line and performSwapWork will hit the SWAPPING guard in cancel() and
        // no-op, giving the runnable exclusive ownership of the session state.
        session.phase = Phase.SWAPPING;

        runOnMainThread(() -> performSwapWork(session));
    }

    private static void performSwapWork(TradeSession session) {
        MinecraftServer server = WorldUtils.server;
        if (server == null) {
            cancelFromSwap(session, Cause.SERVER_STOPPING);
            return;
        }
        ServerPlayer requester = server.getPlayerList().getPlayer(session.requesterId);
        ServerPlayer recipient = server.getPlayerList().getPlayer(session.recipientId);
        if (requester == null || recipient == null) {
            cancelFromSwap(session, Cause.DISCONNECTED);
            return;
        }

        // Pre-flight: simulate delivering each side's payload into the other side's
        // inventory. If either simulation overflows, abort before mutating anything —
        // items remain in their placement containers and cancel() returns them.
        if (!canAcceptAll(requester.getInventory(), session.recipientPlacement)
                || !canAcceptAll(recipient.getInventory(), session.requesterPlacement)) {
            cancelFromSwap(session, Cause.INVENTORY_FULL);
            return;
        }

        for (int i = 0; i < session.recipientPlacement.getContainerSize(); i++) {
            ItemStack stack = session.recipientPlacement.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                requester.getInventory().add(stack);
            }
        }
        for (int i = 0; i < session.requesterPlacement.getContainerSize(); i++) {
            ItemStack stack = session.requesterPlacement.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                recipient.getInventory().add(stack);
            }
        }

        teleportImpl.teleport(
                session.requesterId,
                session.requesterPrev.levelResource(),
                session.requesterPrev.x(),
                session.requesterPrev.y(),
                session.requesterPrev.z(),
                session.requesterPrev.yaw(),
                session.requesterPrev.pitch());
        teleportImpl.teleport(
                session.recipientId,
                session.recipientPrev.levelResource(),
                session.recipientPrev.x(),
                session.recipientPrev.y(),
                session.recipientPrev.z(),
                session.recipientPrev.yaw(),
                session.recipientPrev.pitch());

        Component completed = Component.translatable("factions.trade.result.completed");
        new Message(completed.copy()).send(requester, true);
        new Message(completed.copy()).send(recipient, true);

        SimpleGui rGui = session.requesterGui;
        SimpleGui recGui = session.recipientGui;
        if (rGui != null) rGui.close();
        if (recGui != null) recGui.close();

        SESSIONS.remove(session.sessionId);
        BY_PLAYER.remove(session.requesterId);
        BY_PLAYER.remove(session.recipientId);
        session.phase = Phase.COMPLETED;
    }

    /**
     * Cancel-equivalent cleanup invoked from inside performSwapWork's failure paths.
     * cancel() would treat SWAPPING as terminal-in-progress and no-op, so we flip to
     * CANCELLED and dispatch performCancelWork directly on the tick we already own.
     */
    private static void cancelFromSwap(TradeSession session, Cause cause) {
        session.phase = Phase.CANCELLED;
        performCancelWork(session, cause);
    }

    /** Return the active session containing {@code playerId}, if any. */
    public static Optional<TradeSession> getFor(UUID playerId) {
        UUID sessionId = BY_PLAYER.get(playerId);
        if (sessionId == null) return Optional.empty();
        return Optional.ofNullable(SESSIONS.get(sessionId));
    }

    /** Defensive copy of currently-active sessions — safe to iterate while sessions are cancelled. */
    public static Collection<TradeSession> snapshotAllSessions() {
        return new ArrayList<>(SESSIONS.values());
    }

    /** Cancel every active session with {@link Cause#SERVER_STOPPING}. */
    public static void clearAllForShutdown() {
        for (TradeSession s : snapshotAllSessions()) {
            cancel(s.sessionId, Cause.SERVER_STOPPING);
        }
    }

    /** Task 11 fills in the actual event-listener registrations (disconnect, disband, shutdown). */
    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> {
                    UUID playerId = handler.player.getUUID();
                    TradeSession.getFor(playerId)
                            .ifPresent(
                                    session ->
                                            TradeSession.cancel(session.getId(), Cause.DISCONNECTED));
                });

        FactionEvents.DISBAND.register(
                faction -> {
                    UUID disbandedId = faction.getID();
                    for (TradeSession session : snapshotAllSessions()) {
                        User reqUser = User.get(session.requesterId);
                        User recUser = User.get(session.recipientId);
                        Faction reqFaction = reqUser != null ? reqUser.getFaction() : null;
                        Faction recFaction = recUser != null ? recUser.getFaction() : null;
                        UUID reqFId = reqFaction != null ? reqFaction.getID() : null;
                        UUID recFId = recFaction != null ? recFaction.getID() : null;
                        if (disbandedId.equals(reqFId) || disbandedId.equals(recFId)) {
                            cancel(session.sessionId, Cause.FACTION_DISBANDED);
                        }
                    }
                });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clearAllForShutdown());
    }

    // ---------------------------------------------------------------------- helpers

    private static PreviousPosition capturePreviousPosition(ServerPlayer player) {
        var pos = player.position();
        return new PreviousPosition(
                WorldUtils.dimensionString(player.level()),
                pos.x,
                pos.y,
                pos.z,
                player.getYRot(),
                player.getXRot());
    }

    private static void runOnMainThread(Runnable task) {
        MinecraftServer server = WorldUtils.server;
        if (server != null) {
            server.execute(task);
        } else {
            task.run();
        }
    }

    private static String actionbarKey(Cause cause) {
        return switch (cause) {
            case PLAYER_CANCELLED -> "factions.trade.cancelled.player_cancelled";
            case DECLINED -> "factions.trade.cancelled.declined";
            case INVENTORY_FULL -> "factions.trade.cancelled.inventory_full";
            case DISCONNECTED -> "factions.trade.cancelled.disconnected";
            case GUI_CLOSED -> "factions.trade.cancelled.gui_closed";
            case FACTION_DISBANDED -> "factions.trade.cancelled.faction_disbanded";
            case SERVER_STOPPING -> "factions.trade.cancelled.server_stopping";
        };
    }

    /**
     * For each occupied slot: copy the stack, attempt to add to the player's inventory, drop any
     * remainder at {@code prev}, and clear the slot. The Inventory#add call mutates the passed copy
     * — remaining count > 0 means overflow.
     */
    private static void returnItems(
            SimpleContainer container, ServerPlayer player, PreviousPosition prev) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            player.getInventory().add(copy);
            if (copy.getCount() > 0) {
                dropStackAt(copy, prev);
            }
            container.setItem(i, ItemStack.EMPTY);
        }
    }

    /** For each occupied slot: drop the stack at {@code prev} as an ItemEntity and clear the slot. */
    private static void dropItems(SimpleContainer container, PreviousPosition prev) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            dropStackAt(stack.copy(), prev);
            container.setItem(i, ItemStack.EMPTY);
        }
    }

    private static void dropStackAt(ItemStack stack, PreviousPosition prev) {
        if (stack.isEmpty()) return;
        ServerLevel world = WorldUtils.getWorld(prev.levelResource());
        if (world == null) return;
        ItemEntity entity = new ItemEntity(world, prev.x(), prev.y(), prev.z(), stack);
        world.addFreshEntity(entity);
    }

    /**
     * Non-mutating pre-flight: snapshots the player's inventory into a scratch
     * container and simulates delivering every stack from {@code incoming}. Returns
     * false as soon as one stack does not fit — the real swap is aborted before any
     * mutation occurs.
     */
    private static boolean canAcceptAll(
            net.minecraft.world.entity.player.Inventory playerInv, SimpleContainer incoming) {
        SimpleContainer snapshot = new SimpleContainer(playerInv.getContainerSize());
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            snapshot.setItem(i, playerInv.getItem(i).copy());
        }
        return canFitAll(snapshot, incoming);
    }

    /**
     * Package-private capacity check: simulates delivering every stack from
     * {@code incoming} into {@code snapshot} and returns false at the first
     * overflow. {@code snapshot} MAY be mutated (the caller must own the copy).
     * Exposed for direct testing of the swap invariant without a live
     * {@link net.minecraft.world.entity.player.Inventory}.
     */
    static boolean canFitAll(SimpleContainer snapshot, SimpleContainer incoming) {
        for (int i = 0; i < incoming.getContainerSize(); i++) {
            ItemStack stack = incoming.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStack remainder = addToContainer(snapshot, stack.copy());
            if (!remainder.isEmpty()) return false;
        }
        return true;
    }

    /**
     * Vanilla-parity add: first fill partial stacks of the same item+components, then
     * fall back to an empty slot. Mutates {@code stack} in place; returns the
     * un-placed remainder (empty when everything fit). Uses
     * {@link ItemStack#isSameItemSameComponents} because MC 26.2 replaced the
     * pre-component tag comparator. Package-private for direct testing.
     */
    static ItemStack addToContainer(SimpleContainer c, ItemStack stack) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack existing = c.getItem(i);
            if (!existing.isEmpty()
                    && ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int amount = Math.min(space, stack.getCount());
                existing.grow(amount);
                stack.shrink(amount);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).isEmpty()) {
                c.setItem(i, stack.copy());
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    // ---------------------------------------------------------------------- test-only helpers

    /**
     * Testing only: create and register a session with dummy positions. Bypasses
     * {@link #begin(ServerPlayer, ServerPlayer, TradeStageConfig)}, which requires
     * live {@link ServerPlayer} instances that are not available in unit tests.
     * Callers MUST reset {@link #teleportImpl}, {@link #guiOpenImpl}, and
     * {@link #inventoryImpl} to no-op stubs and clean up the registered session in
     * teardown.
     */
    static TradeSession beginForTest(UUID requesterId, UUID recipientId) {
        PreviousPosition dummy = new PreviousPosition("minecraft:overworld", 0, 0, 0, 0, 0);
        UUID id = UUID.randomUUID();
        TradeSession session =
                new TradeSession(id, requesterId, recipientId, dummy, dummy);
        SESSIONS.put(id, session);
        BY_PLAYER.put(requesterId, id);
        BY_PLAYER.put(recipientId, id);
        return session;
    }
}
