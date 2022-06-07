package io.icker.factions.api.persistents;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;
import org.jetbrains.annotations.Nullable;

@Name("User")
public class User implements Persistent {
    private static final HashMap<UUID, User> STORE = Database.load(User.class, p -> p.getID());

    public enum ChatMode {
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

    @Nullable
    @Field(value = "FactionID")
    private UUID factionID;

    @Nullable
    @Field(value = "Rank")
    private Rank rank;

    @Field("Radar")
    private boolean radar;

    @Field("Chat")
    private ChatMode chat;

    private boolean autoclaim = false;
    private boolean bypass = false;

    public User(UUID id, ChatMode chat, boolean bypass, boolean radar) {
        this.id = id;
        this.chat = chat;
        this.bypass = bypass;
        this.radar = radar;
    }

    public User() { ; }

    public String getKey() {
        return id.toString();
    }

    public static User get(UUID id) {
        if (!STORE.containsKey(id)) {
            User.add(new User(id, ChatMode.GLOBAL, false, false));
        }
        return STORE.get(id);
    }

    public static List<User> getByFaction(UUID factionID) {
        return STORE.values()
            .stream()
            .filter(m -> m.isInFaction() && m.factionID.equals(factionID))
            .toList();
    }

    public static void add(User user) {
        STORE.put(user.id, user);
    }

    public UUID getID() {
        return id;
    }

    public ChatMode getChatMode() {
        return chat;
    }

    public boolean isBypassOn() {
        return bypass;
    }

    public boolean isRadarOn() {
        return radar;
    }

    public boolean isInFaction() {
        return factionID != null;
    }

    public Rank getRank() {
        return rank;
    }

    public String getRankName() {
        return Arrays
            .stream(rank.name().split("_"))
            .map(word -> word.isEmpty() ? word :
                Character.toTitleCase(word.charAt(0)) +
                word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }

    public void setChatMode(ChatMode chat) {
        this.chat = chat;
    }

    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }

    public void setRadar(boolean radar) {
        this.radar = radar;
    }

    public void joinFaction(UUID factionID, Rank rank) {
        this.factionID = factionID;
        this.rank = rank;
        FactionEvents.MEMBER_JOIN.invoker().onMemberJoin(Faction.get(factionID), this);
    }

    public void leaveFaction() {
        UUID oldFactionID = factionID;
        factionID = null;
        rank = null;
        FactionEvents.MEMBER_LEAVE.invoker().onMemberLeave(Faction.get(oldFactionID), this);
    }

    public boolean getAutoclaim() {
        return autoclaim;
    }

    public void toggleAutoclaim() {
        this.autoclaim = !autoclaim;
    }

    public void changeRank(Rank rank) {
        this.rank = rank;
    }

    public static void save() {
        Database.save(User.class, STORE.values().stream().toList());
    }
}