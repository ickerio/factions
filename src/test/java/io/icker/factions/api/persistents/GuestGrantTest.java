package io.icker.factions.api.persistents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

class GuestGrantTest {
    private static int decrementIfAvailable(int remaining) {
        return remaining > 0 ? remaining - 1 : remaining;
    }

    private record TestGrant(UUID factionID, UUID playerID) {
        String key() {
            return factionID + "-" + playerID;
        }
    }

    private static List<TestGrant> getByFaction(
            HashMap<String, TestGrant> store, UUID factionID) {
        return store.values().stream().filter(g -> g.factionID().equals(factionID)).toList();
    }

    @Test
    void breakQuotaDecrementsByOneWhenAvailable() {
        assertEquals(2, decrementIfAvailable(3));
    }

    @Test
    void breakQuotaDoesNotBecomeNegative() {
        assertEquals(0, decrementIfAvailable(0));
    }

    @Test
    void grantIsRevokedWhenBothQuotasReachZero() {
        int breakRemaining = decrementIfAvailable(1);
        int placeRemaining = 0;

        assertTrue(breakRemaining == 0 && placeRemaining == 0);
    }

    @Test
    void grantRemainsWhenPlaceQuotaIsAvailable() {
        int breakRemaining = decrementIfAvailable(1);
        int placeRemaining = 2;

        assertFalse(breakRemaining == 0 && placeRemaining == 0);
    }

    @Test
    void getByFactionReturnsOnlyGrantsForRequestedFaction() {
        UUID factionA = UUID.randomUUID();
        UUID factionB = UUID.randomUUID();
        TestGrant a1 = new TestGrant(factionA, UUID.randomUUID());
        TestGrant a2 = new TestGrant(factionA, UUID.randomUUID());
        TestGrant b1 = new TestGrant(factionB, UUID.randomUUID());

        HashMap<String, TestGrant> store = new HashMap<>();
        store.put(a1.key(), a1);
        store.put(a2.key(), a2);
        store.put(b1.key(), b1);

        List<TestGrant> forA = getByFaction(store, factionA);

        assertEquals(2, forA.size(), "factionA should have exactly its two grants");
        assertTrue(forA.contains(a1), "factionA's a1 must be returned");
        assertTrue(forA.contains(a2), "factionA's a2 must be returned");
        assertFalse(forA.contains(b1), "factionB's grant must NOT be returned for factionA");
    }

    @Test
    void removingEveryGrantForOneFactionLeavesTheOtherFactionsGrantsIntact() {
        UUID disbanded = UUID.randomUUID();
        UUID survivor = UUID.randomUUID();
        TestGrant disbandedGrant1 = new TestGrant(disbanded, UUID.randomUUID());
        TestGrant disbandedGrant2 = new TestGrant(disbanded, UUID.randomUUID());
        TestGrant survivorGrant = new TestGrant(survivor, UUID.randomUUID());

        HashMap<String, TestGrant> store = new HashMap<>();
        store.put(disbandedGrant1.key(), disbandedGrant1);
        store.put(disbandedGrant2.key(), disbandedGrant2);
        store.put(survivorGrant.key(), survivorGrant);

        for (TestGrant grant : getByFaction(store, disbanded)) {
            store.remove(grant.key());
        }

        assertTrue(getByFaction(store, disbanded).isEmpty(),
                "disbanded faction's grants must be gone");
        assertEquals(1, getByFaction(store, survivor).size(),
                "survivor faction's grants must be untouched");
        assertTrue(getByFaction(store, survivor).contains(survivorGrant),
                "survivor's exact grant must remain");
        assertEquals(1, store.size(),
                "store must contain only the survivor's grant after disband cleanup");
    }

}
