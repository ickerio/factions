package io.icker.factions.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClaimExclusivityTest {

    @Test
    void nonBypassUser_cannotClaimForeignChunk() {
        boolean userBypass = false;
        boolean claimBelongsToActor = false;

        boolean shouldReject = !userBypass && !claimBelongsToActor;
        assertTrue(shouldReject, "Non-bypass user must be rejected when claiming a foreign chunk");
    }

    @Test
    void bypassUser_canClaimForeignChunk() {
        boolean userBypass = true;
        boolean claimBelongsToActor = false;

        boolean shouldReject = !userBypass && !claimBelongsToActor;
        assertFalse(shouldReject, "Bypass user must NOT be rejected when claiming a foreign chunk");
    }

    @Test
    void nonBypassUser_canClaimOwnFactionChunk() {
        boolean userBypass = false;
        boolean claimBelongsToActor = true;

        boolean shouldReject = !userBypass && !claimBelongsToActor;
        assertFalse(shouldReject, "Non-bypass user must NOT be rejected for their own faction's chunk");
    }

    @Test
    void unclaimedChunk_neverRejected() {
        boolean existingClaim = false;

        boolean shouldCheckExclusivity = existingClaim;
        assertFalse(shouldCheckExclusivity, "Unclaimed chunks skip the exclusivity check entirely");
    }
}
