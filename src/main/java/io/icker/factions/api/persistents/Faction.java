package io.icker.factions.api.persistents;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Name("Faction")
public class Faction {
    private static final HashMap<UUID, Faction> STORE = Database.load(Faction.class, Faction::getID);
    @Field("Invites")
    public ArrayList<UUID> invites = new ArrayList<>();

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

    @Field("Home")
    private Home home;

    @Field("Safe")
    private SimpleInventory safe = new SimpleInventory(54);

    @Field("Relationships")
    private ArrayList<Relationship> relationships = new ArrayList<>();

    public Faction(String name, String description, String motd, Formatting color, boolean open) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.motd = motd;
        this.description = description;
        this.color = color.getName();
        this.open = open;
    }

    public Faction() {
    }

    public static Faction get(UUID id) {
        return STORE.get(id);
    }

    public static Faction getByName(String name) {
        return STORE.values()
                .stream()
                .filter(f -> f.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    public static void add(Faction faction) {
        STORE.put(faction.id, faction);
    }

    public static Collection<Faction> all() {
        return STORE.values();
    }

    @SuppressWarnings("unused")
    public static List<Faction> allBut(UUID id) {
        return STORE.values()
                .stream()
                .filter(f -> f.id != id)
                .toList();
    }

    @SuppressWarnings("unused")
    public String getKey() {
        return id.toString();
    }

    public UUID getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public Formatting getColor() {
        return Formatting.byName(color);
    }

    public void setColor(Formatting color) {
        this.color = color.getName();
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public String getMOTD() {
        return motd;
    }

    public void setMOTD(String motd) {
        this.motd = motd;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public int getPower() {
        return getUsers().stream().mapToInt(User::getPower).sum();
    }

    public SimpleInventory getSafe() {
        return safe;
    }

    @SuppressWarnings("unused")
    public void setSafe(SimpleInventory safe) {
        this.safe = safe;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public List<User> getUsers() {
        return User.getByFaction(id);
    }

    public List<Claim> getClaims() {
        return Claim.getByFaction(id);
    }

    public void removeAllClaims() {
        Claim.getByFaction(id)
                .forEach(Claim::remove);
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
        return relationships.stream().filter(rel -> rel.target.equals(target)).findFirst().orElse(new Relationship(target, Relationship.Status.NEUTRAL));
    }

    public Relationship getReverse(Relationship rel) {
        return Faction.get(rel.target).getRelationship(id);
    }

    public boolean isMutualAllies(UUID target) {
        Relationship rel = getRelationship(target);
        return rel.status == Relationship.Status.ALLY && getReverse(rel).status == Relationship.Status.ALLY;
    } 

    public List<Relationship> getMutualAllies() {
        return relationships.stream().filter(rel -> isMutualAllies(rel.target)).toList();
    }

    public List<Relationship> getEnemiesWith() {
        return relationships.stream().filter(rel -> rel.status == Relationship.Status.ENEMY).toList();
    }

    @SuppressWarnings("unused") //util
    public List<Relationship> getEnemiesOf() {
        return relationships.stream().filter(rel -> getReverse(rel).status == Relationship.Status.ENEMY).toList();
    }

    public void removeRelationship(UUID target) {
        relationships = new ArrayList<>(relationships.stream().filter(rel -> !rel.target.equals(target)).toList());
    }

    public void setRelationship(@NotNull Relationship relationship) {
        if (getRelationship(relationship.target) != null) {
            removeRelationship(relationship.target);
        }
        if (relationship.status != Relationship.Status.NEUTRAL)
            relationships.add(relationship);
    }

    public void remove() {
        for (User user : getUsers()) {
            user.leaveFaction();
        }
        for (Relationship rel : relationships) {
            Faction.get(rel.target).removeRelationship(id);
        }
        removeAllClaims();
        STORE.remove(id);
        FactionEvents.DISBAND.invoker().onDisband(this);
    }

    public int calculateMaxPower() {
        //we need to remove base power otherwise is problematic
        int maxPower = 0;
        for (final User user : getUsers()) {
            maxPower += user.getMaxPower();
        }
        return maxPower;
    }
        
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Faction faction = (Faction) o;
        return id.equals(faction.id);
    }

    public static void save() {
        Database.save(Faction.class, STORE.values().stream().toList());
    }

    public int calculateRequiredPower(){
        return (getClaims().size()) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
    }
}