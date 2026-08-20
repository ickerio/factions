package io.icker.factions.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: server pushes all faction claims to the client on join / claim change.
 * Wire format (G2 lightweight):
 *   int factionCount
 *   for each faction:
 *     String factionName
 *     int    argbColor
 *     int    dimCount
 *     for each dimension:
 *       String dimensionId      (e.g. "minecraft:overworld")
 *       int    chunkCount
 *       for each chunk: long    (ChunkPos.asLong — one long per chunk)
 */
public record ClaimSyncPayload(List<FactionClaims> factions) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClaimSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("factions", "claim_sync"));

    public record FactionClaims(String name, int argbColor, List<DimClaims> dims) {
        public record DimClaims(String dimensionId, List<Long> chunkLongs) {}
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimSyncPayload> CODEC =
            StreamCodec.of(ClaimSyncPayload::encode, ClaimSyncPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, ClaimSyncPayload payload) {
        buf.writeVarInt(payload.factions().size());
        for (FactionClaims fc : payload.factions()) {
            buf.writeUtf(fc.name());
            buf.writeInt(fc.argbColor());
            buf.writeVarInt(fc.dims().size());
            for (FactionClaims.DimClaims dc : fc.dims()) {
                buf.writeUtf(dc.dimensionId());
                buf.writeVarInt(dc.chunkLongs().size());
                for (long l : dc.chunkLongs()) {
                    buf.writeLong(l);
                }
            }
        }
    }

    private static ClaimSyncPayload decode(RegistryFriendlyByteBuf buf) {
        int factionCount = buf.readVarInt();
        List<FactionClaims> factions = new ArrayList<>(factionCount);
        for (int i = 0; i < factionCount; i++) {
            String name = buf.readUtf();
            int color = buf.readInt();
            int dimCount = buf.readVarInt();
            List<FactionClaims.DimClaims> dims = new ArrayList<>(dimCount);
            for (int d = 0; d < dimCount; d++) {
                String dimId = buf.readUtf();
                int chunkCount = buf.readVarInt();
                List<Long> chunks = new ArrayList<>(chunkCount);
                for (int c = 0; c < chunkCount; c++) {
                    chunks.add(buf.readLong());
                }
                dims.add(new FactionClaims.DimClaims(dimId, chunks));
            }
            factions.add(new FactionClaims(name, color, dims));
        }
        return new ClaimSyncPayload(factions);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
