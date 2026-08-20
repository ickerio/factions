package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class FactionClaimCounts {
    private final Map<UUID, Integer> counts = new HashMap<>();

    int get(UUID factionID) {
        return counts.getOrDefault(factionID, 0);
    }

    void add(UUID factionID) {
        counts.merge(factionID, 1, Integer::sum);
    }

    void replace(UUID previousFactionID, UUID factionID) {
        if (factionID.equals(previousFactionID)) return;

        if (previousFactionID != null) {
            remove(previousFactionID);
        }
        add(factionID);
    }

    void remove(UUID factionID) {
        counts.computeIfPresent(factionID, (id, count) -> count == 1 ? null : count - 1);
    }

    void clear() {
        counts.clear();
    }
}
