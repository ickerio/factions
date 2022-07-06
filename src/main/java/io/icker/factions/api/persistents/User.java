package io.icker.factions.api.persistents;

import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.icker.factions.FactionsMod.CONFIG;
import static io.icker.factions.util.PermissionUtil.getPermissionPower;

@Name("User")
public class User {
    private static final HashMap<UUID, User> STORE = Database.load(User.class, User::getID);

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

    public enum SoundMode {
        NONE,
        WARNINGS,
        FACTION,
        ALL
    }

    @Field("ID")
    private final UUID id;

    @Field("FactionID")
    private UUID factionID;

    @Field("Rank")
    public Rank rank;

    @Field("Power")
    private int power;

//  NOTE(CamperSamu): This should be private and have getters and setters with events in the API
    @Field("Radar")
    public boolean radar = false;

    @Field("Chat")
    public ChatMode chat = ChatMode.GLOBAL;

    @Field("Sounds")
    public SoundMode sounds = SoundMode.ALL;

    public boolean autoclaim = false;
    public boolean bypass = false;
    public String language = "en_us";

    private User spoof;

    public User(UUID id) {
        this.id = id;
        power = getMaxPower();
    }

//    NOTE(CamperSamu): why does this exist??
//    public User() {}

    @SuppressWarnings("unused") //util
    public String getKey() {
        return id.toString();
    }

    public static User get(UUID id) {
        if (!STORE.containsKey(id)) {
            User.add(new User(id));
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

//  NOTE(CamperSamu): Consider refactoring this to uuid(), following the record-like naming convention
    public UUID getID() {
        return id;
    }

    public boolean isInFaction() {
        return factionID != null;
    }

    private String getEnumName(Enum<?> value) {
        return Arrays
                .stream(value.name().split("_"))
                .map(word -> word.isEmpty() ? word :
                        Character.toTitleCase(word.charAt(0)) +
                        word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public String getRankName() {
        return getEnumName(rank);
    }

    public String getChatName() {
        return getEnumName(chat);
    }

    public String getSoundName() {
        return getEnumName(sounds);
    }

    public Faction getFaction() {
        return Faction.get(factionID);
    }

    public User getSpoof() {
        return spoof;
    }

    public void setSpoof(User user) {
        this.spoof = user;
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

    public static void save() {
        Database.save(User.class, STORE.values().stream().toList());
    }

    //region Power Management
    public static int getMaxPower(final @NotNull UUID userUUID) {
        final var user = get(userUUID);
        return user != null ? user.getMaxPower() : 0;
    }

    public int getMaxPower() {
        return CONFIG.POWER.MEMBER + getPermissionPower(getID());
    }

    public static int getPower(final @NotNull UUID userUUID) {
        return get(userUUID).getPower();
    }

    public int getPower() {
        return power;
    }

    @SuppressWarnings("unused") //util
    public static int setPower(final @NotNull UUID userUUID, final int newPower) {
        return get(userUUID).setPower(newPower);
    }

    public int setPower(final int newPower) {
        return power = newPower;
    }

    @SuppressWarnings("unused") //util
    public static int addPower(final @NotNull UUID userUUID, final int powerModifier) {
        return get(userUUID).addPower(powerModifier);
    }

    public int addPower(final int powerModifier) {
        return power += powerModifier;
    }
    //endregion
}