package io.icker.factions.api.persistents;

import java.util.*;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.*;
import io.icker.factions.database.*;
import net.minecraft.util.Formatting;

@Name("Faction")
public class Faction implements Persistent {
    private static final HashMap<UUID, Faction> STORE = Database.load(Faction.class, f -> f.getID());

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

    @Field("Open")
    private boolean open;

    @Field("Power")
    private int power;

    @Child(value = Home.class, nullable = true)
    private Home home;

    @Child(value = Invite.class, list = true)
    private ArrayList<Invite> invites;

    public Faction(String name, String description, String motd, Formatting color, boolean open, int power) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.motd = motd;
        this.description = description;
        this.color = color.getName();
        this.open = open;
        this.power = power;
    }

    public Faction() { ; }

    public String getKey() {
        return id.toString();
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

    public static List<Faction> allBut(UUID id) {
        return STORE.values()
            .stream()
            .filter(f -> f.id != id)
            .toList();
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
        return power;
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
        int maxPower = FactionsMod.CONFIG.BASE_POWER + (getUsers().size() * FactionsMod.CONFIG.MEMBER_POWER);
        int newPower = Math.min(Math.max(0, power + adjustment), maxPower);
        int oldPower = this.power;

        power = newPower;
        FactionEvents.POWER_CHANGE.invoker().onPowerChange(this, oldPower);
        return Math.abs(newPower - oldPower);
    }

    public List<User> getUsers() {
        return User.getByFaction(id);
    }

    public List<Claim> getClaims() {
        return Claim.getByFaction(id);
    }

    public void removeAllClaims() {
        Claim.getByFaction(id)
            .stream()
            .forEach(c -> c.remove());
        FactionEvents.REMOVE_ALL_CLAIMS.invoker().onRemoveAllClaims(this);
    }

    public void addClaim(int x, int z, String level) {
        Claim.add(new Claim(x, z, level, id));
    }

    public ArrayList<Invite> getInvites() {
        return invites;
    }

    public Invite getInvite(UUID playerID) {
        return invites.stream().filter((invite) -> invite.getPlayerID() == playerID).findFirst().get();
    }

    public void addInvite(Invite invite) {
        this.invites.add(invite);
    }

    public void removeInvite(Invite invite) {
        this.invites.remove(invite);
    }

    public Home getHome() {
        return home;
    }

    public void setHome(Home home) {
        this.home = home;
    }

    public void remove() {
        for (User user : getUsers()) {
            user.leaveFaction();
        }
        for (Relationship rel : Relationship.getByFaction(id)) {
            rel.getReverse().remove();
            rel.remove();
        }
        removeAllClaims();
        STORE.remove(id);
        FactionEvents.DISBAND.invoker().onDisband(this);
    }

    public static void save() {
        Database.save(Faction.class, STORE.values().stream().toList());
    }
}