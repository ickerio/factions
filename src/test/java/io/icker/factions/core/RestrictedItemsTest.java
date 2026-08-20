package io.icker.factions.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class RestrictedItemsTest {
    /**
     * Mirrors the pure ID-in-list matching logic of {@link InteractionManager#isRestrictedForGuests}
     * without touching Minecraft registries or {@code FactionsMod.CONFIG}, which throw under
     * FabricLoader in the unit-test environment.
     */
    private static boolean matches(String id, List<String> restricted) {
        if (id == null || id.isEmpty()) return false;
        if (restricted == null || restricted.isEmpty()) return false;
        return restricted.contains(id);
    }

    private static List<String> defaultList() {
        return new ArrayList<>(
                List.of(
                        "minecraft:flint_and_steel",
                        "minecraft:fire_charge",
                        "minecraft:lava_bucket"));
    }

    @Test
    void idInRestrictedListMatches() {
        List<String> restricted = defaultList();

        assertTrue(matches("minecraft:flint_and_steel", restricted));
        assertTrue(matches("minecraft:fire_charge", restricted));
        assertTrue(matches("minecraft:lava_bucket", restricted));
    }

    @Test
    void idNotInRestrictedListDoesNotMatch() {
        List<String> restricted = defaultList();

        assertFalse(matches("minecraft:stone", restricted));
        assertFalse(matches("minecraft:water_bucket", restricted));
        assertFalse(matches("minecraft:diamond_sword", restricted));
    }

    @Test
    void emptyRestrictedListMatchesNothing() {
        List<String> restricted = new ArrayList<>();

        assertFalse(matches("minecraft:flint_and_steel", restricted));
        assertFalse(matches("minecraft:lava_bucket", restricted));
    }

    @Test
    void nullRestrictedListMatchesNothing() {
        assertFalse(matches("minecraft:flint_and_steel", null));
        assertFalse(matches("minecraft:lava_bucket", null));
    }

    @Test
    void matchingIsExactAndDoesNotPrefixMatch() {
        List<String> restricted = defaultList();

        assertFalse(
                matches("minecraft:lava_bucket_thing", restricted),
                "lava_bucket must not match a longer id that starts with it");
        assertFalse(
                matches("minecraft:flint_and_steele", restricted),
                "flint_and_steel must not match a trailing typo");
        assertFalse(
                matches("modname:lava_bucket", restricted),
                "namespaced ids from another mod must not match vanilla ids");
    }

    @Test
    void nullOrEmptyIdDoesNotMatch() {
        List<String> restricted = defaultList();

        assertFalse(matches(null, restricted));
        assertFalse(matches("", restricted));
    }
}
