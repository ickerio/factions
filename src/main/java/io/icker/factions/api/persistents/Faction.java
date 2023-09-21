package io.icker.factions.api.persistents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.util.WorldUtils;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

@Name("Faction")
public class Faction {
    private static final HashMap<UUID, Faction> STORE =
            Database.load(Faction.class, Faction::getID);

    @Field("ID")
    private UUID id;

    @Field("Name")
    private String name;

    @Field("Description")
    private String description;

    @Field("MOTD")
    private String motd;

    @Field("Color")
    private String color;

    /**
     * Whether a player can join without an invitation
     */
    @Field("Open")
    private boolean open;

    @Field("Power")
    private int power;

    @Field("AdminPower")
    private int adminPower;

    @Field("Home")
    private Home home;

    @Field("Safe")
    private SimpleInventory safe = new SimpleInventory(54);

    @Field("Invites")
    public ArrayList<UUID> invites = new ArrayList<>();

    @Field("Relationships")
    private ArrayList<Relationship> relationships = new ArrayList<>();

    @Field("GuestPermissions")
    public ArrayList<Relationship.Permissions> guest_permissions =
            new ArrayList<>(FactionsMod.CONFIG.RELATIONSHIPS.DEFAULT_GUEST_PERMISSIONS);

    public Faction(String name, String description, String motd, Formatting color, boolean open,
            int power) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.motd = motd;
        this.description = description;
        this.color = color.getName();
        this.open = open;
        this.power = power;
    }

    @SuppressWarnings("unused")
    public Faction() {}

    @SuppressWarnings("unused")
    public String getKey() {
        return id.toString();
    }

    @Nullable
    public static Faction get(UUID id) {
        return STORE.get(id);
    }

    @Nullable
    public static Faction getByName(String name) {
        return STORE.values().stream().filter(f -> f.name.equals(name)).findFirst().orElse(null);
    }

    public static void add(Faction faction) {
        STORE.put(faction.id, faction);
    }

    public static Collection<Faction> all() {
        return STORE.values();
    }

    @SuppressWarnings("unused")
    public static List<Faction> allBut(UUID id) {
        return STORE.values().stream().filter(f -> f.id != id).toList();
    }

    public UUID getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Formatting getColor() {
        return Formatting.byName(color);
    }

    public String getDescription() {
        return description;
    }

    public String getMOTD() {
        return motd;
    }

    public int getPower() {
        return power + adminPower;
    }

    public SimpleInventory getSafe() {
        return safe;
    }

    public DefaultedList<ItemStack> clearSafe() {
        DefaultedList<ItemStack> stacks = this.safe.stacks;
        this.safe = new SimpleInventory(54);
        return stacks;
    }

    public boolean isOpen() {
        return open;
    }

    public void setName(String name) {
        this.name = name;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public void setDescription(String description) {
        this.description = description;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public void setMOTD(String motd) {
        this.motd = motd;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public void setColor(Formatting color) {
        this.color = color.getName();
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public void setOpen(boolean open) {
        this.open = open;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public int adjustPower(int adjustment) {
        int maxPower = calculateMaxPower();
        int newPower = Math.min(Math.max(0, power + adjustment), maxPower);
        int oldPower = this.power;

        if (newPower == oldPower)
            return 0;

        power = newPower;
        FactionEvents.POWER_CHANGE.invoker().onPowerChange(this, oldPower);
        return Math.abs(newPower - oldPower);
    }

    public int getAdminPower() {
        return adminPower;
    }

    public void addAdminPower(int amount) {
        adminPower += amount;
    }

    public List<User> getUsers() {
        return User.getByFaction(id);
    }

    public List<Claim> getClaims() {
        return Claim.getByFaction(id);
    }

    public void removeAllClaims() {
        Claim.getByFaction(id).stream().forEach(Claim::remove);
        FactionEvents.REMOVE_ALL_CLAIMS.invoker().onRemoveAllClaims(this);
    }

    public void addClaim(int x, int z, String level) {
        Claim.add(new Claim(x, z, level, id));
    }

    public boolean isInvited(UUID playerID) {
        return invites.stream().anyMatch(invite -> invite.equals(playerID));
    }

    public Home getHome() {
        return home;
    }

    public void setHome(Home home) {
        this.home = home;
        FactionEvents.SET_HOME.invoker().onSetHome(this, home);
    }

    public Relationship getRelationship(UUID target) {
        return relationships.stream().filter(rel -> rel.target.equals(target)).findFirst()
                .orElse(new Relationship(target, Relationship.Status.NEUTRAL));
    }

    public Relationship getReverse(Relationship rel) {
        return Faction.get(rel.target).getRelationship(id);
    }

    public boolean isMutualAllies(UUID target) {
        Relationship rel = getRelationship(target);
        return rel.status == Relationship.Status.ALLY
                && getReverse(rel).status == Relationship.Status.ALLY;
    }

    public List<Relationship> getMutualAllies() {
        return relationships.stream().filter(rel -> isMutualAllies(rel.target)).toList();
    }

    public List<Relationship> getEnemiesWith() {
        return relationships.stream().filter(rel -> rel.status == Relationship.Status.ENEMY)
                .toList();
    }

    public List<Relationship> getEnemiesOf() {
        return relationships.stream()
                .filter(rel -> getReverse(rel).status == Relationship.Status.ENEMY).toList();
    }

    public void removeRelationship(UUID target) {
        relationships = new ArrayList<>(
                relationships.stream().filter(rel -> !rel.target.equals(target)).toList());
    }

    public void setRelationship(Relationship relationship) {
        if (getRelationship(relationship.target) != null) {
            removeRelationship(relationship.target);
        }
        if (relationship.status != Relationship.Status.NEUTRAL
                || !relationship.permissions.isEmpty())
            relationships.add(relationship);
    }

    public void remove() {
        for (User user : getUsers()) {
            user.leaveFaction();
        }
        for (Relationship rel : relationships) {
            Faction target = Faction.get(rel.target);
            if (target != null) {
                target.removeRelationship(id);
            }
        }
        removeAllClaims();
        STORE.remove(id);
        FactionEvents.DISBAND.invoker().onDisband(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Faction faction = (Faction) o;
        return id.equals(faction.id);
    }

    public static void audit() {
        STORE.values().removeIf((faction) -> {
            if (faction.home != null && !WorldUtils.isValid(faction.home.level)) {
                faction.setHome(null);
            }

            faction.relationships.removeIf((rel) -> Faction.get(rel.target) == null);

            return faction.getUsers().stream().noneMatch((user) -> user.rank == User.Rank.OWNER);
        });
    }

    public static void save() {
        Database.save(Faction.class, STORE.values().stream().toList());
    }

    // TODO(samu): import per-player power patch
    public int calculateMaxPower() {
        return FactionsMod.CONFIG.POWER.BASE
                + (getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER);
    }
}
