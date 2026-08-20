package io.icker.factions.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaimSyncPayloadTest {
    @Test
    void roundTrip() {
        var dimA = new ClaimSyncPayload.FactionClaims.DimClaims(
                "minecraft:overworld",
                List.of(ChunkPos.pack(0, 0), ChunkPos.pack(1, -1)));
        var dimB = new ClaimSyncPayload.FactionClaims.DimClaims(
                "minecraft:the_nether",
                List.of(ChunkPos.pack(5, 5)));
        var faction1 = new ClaimSyncPayload.FactionClaims("Alpha", 0xFF0000FF, List.of(dimA, dimB));
        var faction2 = new ClaimSyncPayload.FactionClaims("Beta", 0xFF00FF00, List.of(dimA));
        var original = new ClaimSyncPayload(List.of(faction1, faction2));

        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
        ClaimSyncPayload.CODEC.encode(buf, original);
        var decoded = ClaimSyncPayload.CODEC.decode(buf);

        assertEquals(original, decoded);
    }
}
