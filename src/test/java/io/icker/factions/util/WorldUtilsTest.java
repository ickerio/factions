package io.icker.factions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import org.junit.jupiter.api.Test;

class WorldUtilsTest {
    @Test
    void chunkPositionUsesBlockCoordinatesWithoutAWorld() {
        BlockPos blockPosition = new BlockPos(-1, 64, 16);

        ChunkPos chunkPosition = WorldUtils.getChunkPos(blockPosition);

        assertEquals(-1, chunkPosition.x());
        assertEquals(1, chunkPosition.z());
    }
}
