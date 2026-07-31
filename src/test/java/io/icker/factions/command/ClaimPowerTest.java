package io.icker.factions.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mirrors the pure arithmetic of the multi-chunk claim power rule that lives inside
 * {@code ClaimCommand.addForced}. The command itself needs a live Minecraft server, so
 * the assertions here focus on the affordability inequality — the same one the
 * {@link ClaimExclusivityTest} and {@code GuestGrantTest} suites replicate locally
 * to sidestep the FabricLoader-only runtime.
 *
 * <p>Rule (as of 3.1.1):
 * <pre>
 *   (claimCount + newChunks) * claimWeight &lt;= maxPower
 * </pre>
 * where {@code newChunks} counts ONLY chunks where {@code existingClaim == null} —
 * chunks already owned by the acting faction are re-claimed harmlessly and do NOT
 * contribute to the power cost.
 */
class ClaimPowerTest {

    /** The new (3.1.1) affordability rule that gates {@code /f claim add <size>}. */
    private static boolean canAffordNewRule(
            int claimCount, int newChunks, int claimWeight, int maxPower) {
        int required = (claimCount + newChunks) * claimWeight;
        return maxPower >= required;
    }

    /**
     * The old (3.1.0) rule that only charged for a single chunk regardless of the
     * requested size. Kept solely to prove the exploit case now fails.
     */
    private static boolean canAffordOldRule(int claimCount, int claimWeight, int maxPower) {
        int required = (claimCount + 1) * claimWeight;
        return maxPower >= required;
    }

    /** {@code (2 * size - 1)^2} — the chunk-square footprint of {@code /f claim add <size>}. */
    private static int squareFootprint(int size) {
        int side = 2 * size - 1;
        return side * side;
    }

    @Test
    void sufficientPowerAllowsSingleClaim() {
        // Given a faction with plenty of headroom
        int claimCount = 3;
        int newChunks = 1;
        int weight = 5;
        int maxPower = 100;

        // When the new rule is evaluated
        boolean afford = canAffordNewRule(claimCount, newChunks, weight, maxPower);

        // Then the claim is allowed: (3 + 1) * 5 = 20 <= 100
        assertTrue(afford, "20 required vs 100 max should be affordable");
    }

    @Test
    void insufficientPowerRejectsMultiChunkClaim() {
        // Given a broke faction attempting `/f claim add 3` on entirely new land
        int claimCount = 0;
        int newChunks = squareFootprint(3); // 5x5 = 25
        int weight = 5;
        int maxPower = 40;

        // When the new rule evaluates it
        boolean afford = canAffordNewRule(claimCount, newChunks, weight, maxPower);

        // Then the claim is rejected: (0 + 25) * 5 = 125 > 40
        assertFalse(afford, "125 required vs 40 max should NOT be affordable");
    }

    @Test
    void multiChunkExploitOldRulePassedNewRuleRejects() {
        // Given the exact exploit case from the bug report: /f claim add 7 on empty land
        // with a tiny amount of power. `size=7` => (2*7-1)^2 = 169 chunks.
        int claimCount = 0;
        int newChunks = squareFootprint(7); // 13x13 = 169
        int weight = 1;
        int maxPower = 10;

        // When the old rule is evaluated
        boolean oldRuleAllowed = canAffordOldRule(claimCount, weight, maxPower);

        // When the new rule is evaluated
        boolean newRuleAllowed = canAffordNewRule(claimCount, newChunks, weight, maxPower);

        // Then the old rule (buggy) passed but the new rule (fixed) blocks it
        assertTrue(oldRuleAllowed, "old rule (buggy): (0+1)*1 = 1 <= 10 — WOULD have passed");
        assertFalse(newRuleAllowed, "new rule (fixed): (0+169)*1 = 169 > 10 — MUST fail");
    }

    @Test
    void alreadyOwnedChunksDoNotCountTowardCost() {
        // Given a 3x3 (size=2) claim where 5 of the 9 chunks are already owned by the actor's
        // faction — those must be re-claimed harmlessly and contribute 0 to the power cost.
        int claimCount = 5; // the pre-owned chunks are counted in the current tally
        int totalChunksInSquare = squareFootprint(2); // 3x3 = 9
        int reOwnedChunks = 5;
        int newChunks = totalChunksInSquare - reOwnedChunks; // 4 genuinely new
        int weight = 5;
        int maxPower = 50;

        // When the new rule counts only new chunks
        boolean afford = canAffordNewRule(claimCount, newChunks, weight, maxPower);

        // Then the cost is (5 + 4) * 5 = 45 <= 50 — allowed
        assertTrue(afford, "already-owned chunks must NOT be double-charged");

        // Sanity check: if we (wrongly) counted the re-owned chunks too, we would fail
        boolean wouldFailIfWeDoubleCharged =
                !canAffordNewRule(claimCount, totalChunksInSquare, weight, maxPower);
        assertTrue(
                wouldFailIfWeDoubleCharged,
                "the fixture MUST distinguish re-owned from new — (5+9)*5=70>50");
    }

    @Test
    void singleClaimBehavesIdenticallyToOldRule() {
        // Given a size=1 add (`/f claim add`), which claims exactly one chunk when it's new
        int claimCount = 4;
        int newChunks = 1;
        int weight = 5;

        // When both rules evaluate a range of power budgets around the threshold
        // Threshold: (4 + 1) * 5 = 25. Old rule uses the same formula.
        for (int maxPower : new int[] {0, 10, 24, 25, 26, 100}) {
            boolean newRule = canAffordNewRule(claimCount, newChunks, weight, maxPower);
            boolean oldRule = canAffordOldRule(claimCount, weight, maxPower);

            // Then single-chunk claims must give the same verdict — no regression
            assertTrue(
                    newRule == oldRule,
                    "size=1 must match old rule at maxPower=" + maxPower);
        }
    }

    @Test
    void singleClaimOnAlreadyOwnedChunkCostsZeroNewChunks() {
        // Given `/f claim add` on the actor's own already-owned chunk (an admin re-claim
        // via bypass, for instance) — newChunks should be 0.
        int claimCount = 10;
        int newChunks = 0;
        int weight = 5;
        int maxPower = 50; // exactly at the current claim tally cost

        // When the new rule evaluates it
        boolean afford = canAffordNewRule(claimCount, newChunks, weight, maxPower);

        // Then (10 + 0) * 5 = 50 <= 50 — allowed, and no additional power is charged
        assertTrue(afford, "re-claiming with 0 new chunks charges no additional power");
    }

    @Test
    void exactThresholdIsAffordable() {
        // Given a claim that would consume power exactly equal to the maximum
        int claimCount = 2;
        int newChunks = 3;
        int weight = 4;
        int maxPower = 20; // (2 + 3) * 4 = 20

        // When the new rule evaluates it
        boolean afford = canAffordNewRule(claimCount, newChunks, weight, maxPower);

        // Then required == max is allowed (not strictly greater than)
        assertTrue(afford, "required == max must be affordable, not rejected");
    }
}
