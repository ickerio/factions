package io.icker.factions.client;

import io.icker.factions.net.ClaimSyncPayload;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
            int argb = 0x45000000 | rgb;
            for (ClaimSyncPayload.FactionClaims.DimClaims dc : fc.dims()) {
                List<PackedRect> rects = newMap.computeIfAbsent(dc.dimensionId(), k -> new ArrayList<>());
                mergeIntoRects(dc.chunkLongs(), argb, rects);
            }
        }
        newMap.replaceAll((k, v) -> Collections.unmodifiableList(v));
        byDim = Collections.unmodifiableMap(newMap);
    }

    // Greedy rectilinear cover: sweep row-major, grow each seed rightward
    // then downward while every column is still in the set. O(n * avgArea).
    private static void mergeIntoRects(List<Long> chunks, int argb, List<PackedRect> out) {
        if (chunks.isEmpty()) return;
        HashSet<Long> remaining = new HashSet<>(chunks.size() * 2);
        remaining.addAll(chunks);
        List<Long> sorted = new ArrayList<>(chunks);
        sorted.sort((a, b) -> {
            int az = ChunkPos.getZ(a), bz = ChunkPos.getZ(b);
            if (az != bz) return Integer.compare(az, bz);
            return Integer.compare(ChunkPos.getX(a), ChunkPos.getX(b));
        });
        for (Long seed : sorted) {
            if (!remaining.contains(seed)) continue;
            int cx = ChunkPos.getX(seed);
            int cz = ChunkPos.getZ(seed);
            int w = 1;
            while (remaining.contains(ChunkPos.pack(cx + w, cz))) w++;
            int h = 1;
            growH: while (true) {
                for (int i = 0; i < w; i++) {
                    if (!remaining.contains(ChunkPos.pack(cx + i, cz + h))) break growH;
                }
                h++;
            }
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    remaining.remove(ChunkPos.pack(cx + i, cz + j));
                }
            }
            out.add(new PackedRect(
                    cx << 4,
                    cz << 4,
                    ((cx + w) << 4) - 1,
                    ((cz + h) << 4) - 1,
                    argb));
        }
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
