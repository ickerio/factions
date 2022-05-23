package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

@Name("Member")
public class Member implements Persistent {
    private static final HashMap<UUID, Member> STORE = Database.load(Member.class, p -> p.getID());

    public enum ChatOption {
        FOCUS,
        FACTION,
        GLOBAL
    }

    public enum Rank {
        OWNER,
        LEADER,
        COMMANDER,
        MEMBER
    }

    @Field("ID")
    private UUID id;

    @Field("Chat")
    private ChatOption chat;

    @Field("Bypass")
    private boolean bypass;

    @Field("ZoneMessage")
    private boolean zoneMessage;

    @Field("FactionID")
    private UUID factionID;
    
    @Field("Rank")
    private Rank rank;

    public Member(UUID id, ChatOption chat, boolean bypass, boolean zoneMessage) {
        this.id = id;
        this.chat = chat;
        this.bypass = bypass;
        this.zoneMessage = zoneMessage;
    }

    public String getKey() {
        return id.toString();
    }

    public static Member get(UUID id) {
        return STORE.get(id);
    }

    public static List<Member> getByFaction(UUID factionID) {
        return STORE.values()
            .stream()
            .filter(m -> m.factionID == factionID)
            .collect(Collectors.toList());
    }

    public static void add(Member member) {
        STORE.put(member.id, member);
    }

    public UUID getID() {
        return id;
    }

    public ChatOption getChatOption() {
        return chat;
    }

    public boolean isBypassOn() {
        return bypass;
    }

    public boolean isZoneOn() {
        return zoneMessage;
    }

    public boolean isInFaction() {
        return factionID != null;
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }

    public void setChatOption(ChatOption chat) {
        this.chat = chat;
    }

    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }

    public void setZoneMessage(boolean zoneMessage) {
        this.zoneMessage = zoneMessage;
    }

    public void joinFaction(UUID factionID, Rank rank) {
        this.factionID = factionID;
        this.rank = rank;
        // TODO AddMemberEvent -> JoinFactionEvent
    }

    public void leaveFaction() {
        factionID = null;
        rank = null;
        // TODO RemoveMemberEvent -> LeaveFactionEvent
    }
}