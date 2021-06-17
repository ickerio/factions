package io.icker.factions.database;

import java.util.ArrayList;
import java.util.UUID;

public class Invite {
    private UUID playerId;
    private String factionName;

    public static ArrayList<Invite> get(UUID playerId) {
        Query query = new Query("SELECT faction FROM Invite WHERE player = ?;")
            .set(playerId)
            .executeQuery();

        if (!query.success) return null;
        ArrayList<Invite> invites = new ArrayList<Invite>();

        while (query.next()) {
            invites.add(new Invite(playerId, query.getString("faction")));
        }
        return invites;
    }

    public static ArrayList<Invite> get(String factionName) {
        Query query = new Query("SELECT player FROM Invite WHERE faction = ?;")
            .set(factionName)
            .executeQuery();

        if (!query.success) return null;
        ArrayList<Invite> invites = new ArrayList<Invite>();

        while (query.next()) {
            invites.add(new Invite((UUID) query.getObject("player"), factionName));
        }
        return invites;
    }

    public static Invite add(UUID playerId, String factionName) {
        Query query = new Query("INSERT INTO Invite VALUES (?, ?);")
            .set(playerId, factionName)
            .executeUpdate();

        if (!query.success) return null;
        return new Invite(playerId, factionName);
    }

    public Invite(UUID playerId, String factionName) {
        this.playerId = playerId;
        this.factionName = factionName;
    }

    public Member getMember() {
        return Member.get(playerId);
    }

    public Faction getFaction() {
        return Faction.get(factionName);
    }
}
