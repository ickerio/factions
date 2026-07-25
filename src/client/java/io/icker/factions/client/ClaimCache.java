package io.icker.factions.client;

import io.icker.factions.net.ClaimSyncPayload;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side cache of all faction claims.
 * Populated on receipt of ClaimSyncPayload. Cleared on disconnect.
 *
 * Render hot path: zero allocation — just iterate prebuilt PackedRect lists.
 */
public final class ClaimCache {
    private ClaimCache() {}

    /**
     * Precomputed render data per dimension.
     * Key = dimension id string (e.g. "minecraft:overworld").
     * Value = list of PackedRect, one per claimed chunk per faction.
     * Replaced atomically on each payload receipt.
     */
    private static volatile Map<String, List<PackedRect>> byDim = Map.of();

    /** Called from ClientPlayNetworking receiver — runs on the Netty IO thread, then schedules to client thread. */
    public static void update(ClaimSyncPayload payload) {
        Map<String, List<PackedRect>> newMap = new HashMap<>();
        for (ClaimSyncPayload.FactionClaims fc : payload.factions()) {
            int rgb = fc.argbColor() & 0x00FFFFFF;
            int argb = 0x40000000 | rgb;
            for (ClaimSyncPayload.FactionClaims.DimClaims dc : fc.dims()) {
                List<PackedRect> rects = newMap.computeIfAbsent(dc.dimensionId(), k -> new ArrayList<>());
                for (long packed : dc.chunkLongs()) {
                    int cx = ChunkPos.getX(packed);
                    int cz = ChunkPos.getZ(packed);
                    // Precompute block-coord rect so render path does zero arithmetic
                    rects.add(new PackedRect(
                            cx << 4,
                            cz << 4,
                            (cx << 4) + 15,
                            (cz << 4) + 15,
                            argb));
                }
            }
        }
        // Wrap lists as unmodifiable so renderers can read without synchronisation concerns
        newMap.replaceAll((k, v) -> Collections.unmodifiableList(v));
        byDim = Collections.unmodifiableMap(newMap);
    }

    /** Called on ClientPlayConnectionEvents.DISCONNECT. */
    public static void clear() {
        byDim = Map.of();
    }

    /**
     * Returns the precomputed rect list for the given dimension, or an empty list.
     * Safe to call from render thread — volatile read + unmodifiable list.
     */
    public static List<PackedRect> getForDim(String dimensionId) {
        return byDim.getOrDefault(dimensionId, List.of());
    }

    /** Immutable, allocation-free render data for one claimed chunk. */
    public record PackedRect(int minX, int minZ, int maxX, int maxZ, int argb) {}
}
