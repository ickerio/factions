package io.icker.factions.api.persistents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Faction.Rules}.
 *
 * <p>{@code Faction.Rules} is a plain nested POJO — no persistence side effects on construction
 * — and holds the per-faction toggles that the interaction listeners consult. This suite pins
 * the elytra rule's default value and its mutability, which is the pure-Java half of the
 * "elytra restricted in own territory" feature added in 3.3.
 *
 * <p>The {@link Faction#isElytraAllowed()} accessor itself is trivial delegation
 * ({@code return rules != null && rules.elytra}) but constructing a {@link Faction} instance
 * loads the outer class's static store from disk via {@code Database.load(...)}, which is only
 * available at runtime under FabricLoader. That is covered by manual F3 QA — this suite pins
 * the POJO invariant that the accessor depends on.
 */
class FactionRulesElytraTest {

    @Test
    void rules_elytra_defaultTrue() {
        Faction.Rules rules = new Faction.Rules();

        assertTrue(rules.elytra,
                "elytra must default to true — the 3.3 rule is opt-out, not opt-in, so "
                        + "an existing faction with no persisted Rules block continues to "
                        + "allow elytra flight until the leader disables it");
    }

    @Test
    void rules_elytra_isMutable() {
        Faction.Rules rules = new Faction.Rules();

        rules.elytra = false;

        assertFalse(rules.elytra,
                "the field is deliberately public so the /f rule set command can flip it "
                        + "without a setter — pinning that access remains open");
    }

    @Test
    void rules_elytra_isMutableBackToTrue() {
        Faction.Rules rules = new Faction.Rules();
        rules.elytra = false;

        rules.elytra = true;

        assertTrue(rules.elytra,
                "flipping back must be symmetric — the leader can re-enable elytra later");
    }

    @Test
    void rules_defaultConstructor_producesNonNullInstance() {
        Faction.Rules rules = new Faction.Rules();

        assertNotNull(rules,
                "the no-arg constructor must produce a live Rules instance — this is what "
                        + "Faction uses when initialising the {@code rules} field on new "
                        + "factions and when reading legacy on-disk data that has no Rules "
                        + "block");
    }

    @Test
    void rules_independentInstances_doNotShareState() {
        Faction.Rules a = new Faction.Rules();
        Faction.Rules b = new Faction.Rules();

        a.elytra = false;

        assertTrue(b.elytra,
                "each Rules instance owns its own state — flipping one MUST NOT bleed into "
                        + "another (guards against an accidental static field regression)");
        assertFalse(a.elytra,
                "the flip on `a` must have persisted on `a`");
    }
}
