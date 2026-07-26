package io.icker.factions.api.persistents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FactionClaimCountsTest {
    @Test
    void claimCountTracksAddsAndRemovalsWithoutWorldAccess() {
        UUID factionId = UUID.randomUUID();
        FactionClaimCounts counts = new FactionClaimCounts();

        counts.add(factionId);
        assertEquals(1, counts.get(factionId));

        counts.remove(factionId);
        assertEquals(0, counts.get(factionId));
    }

    @Test
    void replacingAClaimKeepsFactionCountsAccurate() {
        UUID firstFactionId = UUID.randomUUID();
        UUID secondFactionId = UUID.randomUUID();
        FactionClaimCounts counts = new FactionClaimCounts();

        counts.add(firstFactionId);
        counts.replace(firstFactionId, secondFactionId);

        assertEquals(0, counts.get(firstFactionId));
        assertEquals(1, counts.get(secondFactionId));
    }

    @Test
    void replacingAClaimForTheSameFactionKeepsItsCount() {
        UUID factionId = UUID.randomUUID();
        FactionClaimCounts counts = new FactionClaimCounts();

        counts.add(factionId);
        counts.replace(factionId, factionId);

        assertEquals(1, counts.get(factionId));
    }
}
