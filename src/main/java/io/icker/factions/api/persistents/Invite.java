package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

@Name("Invite")
public class Invite implements Persistent {
    private static final HashMap<String, Invite> STORE = Database.load(Invite.class, i -> i.getKey());

    @Field("PlayerID")
    private UUID playerID;

    @Field("FactionID")
    private UUID factionID;

    public Invite(UUID playerID, UUID factionID) {
        this.playerID = playerID;
        this.factionID = factionID;
    }

    public String getKey() {
        return playerID.toString() + "-" + factionID.toString();
    }

    public static Invite get(UUID playerID, UUID factionID) {
        return STORE.get(playerID.toString() + "-" + factionID.toString());
    }

    public UUID getPlayer() {
        return playerID;
    }

    public static List<Invite> getByPlayer(UUID playerID) {
        return STORE.values()
            .stream()
            .filter(i -> i.playerID == playerID)
            .collect(Collectors.toList());
    }

    public static List<Invite> getByFaction(UUID factionID) {
        return STORE.values()
            .stream()
            .filter(i -> i.factionID == factionID)
            .collect(Collectors.toList());
    }

    public static void add(Invite invite) {
        STORE.put(invite.getKey(), invite);
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }

    public void remove() {
        STORE.remove(getKey());
    }
}
