package io.icker.factions.util;

import io.icker.factions.database.Faction;

public class FactionSuggestions {
    public static String[] suggestions() {
      return Faction.all().stream().map(a -> a.name).toArray(String[]::new);
    }
}