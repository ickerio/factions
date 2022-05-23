package io.icker.factions.api.persistents;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.AddMemberEvent;
import io.icker.factions.api.events.RemoveMemberEvent;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;
import io.icker.factions.event.FactionEvents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Rank.valueOf(query.getString("rank").toUpperCase()

@Name("Member")
public class Member implements Persistent {
    private static final HashMap<UUID, Member> STORE = Database.load(Member.class, m -> m.getPlayerID());

    public enum Rank {
        OWNER,
        LEADER,
        COMMANDER,
        MEMBER
    }

    @Field("PlayerID")
    private UUID playerID;

    @Field("FactionID")
    private UUID factionID;

    @Field("Rank")
    private Rank rank;

    public Member(UUID playerID, UUID factionID, Rank rank) {
        this.playerID = playerID;
        this.factionID = factionID;
        this.rank = rank;
    }

    public String getKey() {
        return playerID.toString();
    }

    public static Member get(UUID playerID) {
        return STORE.get(playerID);
    }

    public static List<Member> getByFaction(UUID factionID) {
        return STORE.values()
            .stream()
            .filter(m -> m.factionID == factionID)
            .collect(Collectors.toList());
    }

    public static void add(Member member) {
        STORE.put(member.playerID, member);
        AddMemberEvent.run(member);
        FactionEvents.updatePlayerList(FactionsMod.playerManager.getPlayer(member.playerID));
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }

    public Rank getRank() {
        return rank;
    }

    public void updateRank(Rank rank) {
        this.rank = rank;
    }

    public void remove() {
        STORE.remove(playerID);
        RemoveMemberEvent.run(this);
    }
}