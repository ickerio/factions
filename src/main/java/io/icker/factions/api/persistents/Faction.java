package io.icker.factions.api.persistents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.core.ChatManager;
import io.icker.factions.core.FactionsManager;
import io.icker.factions.core.ServerManager;
import io.icker.factions.core.WorldManager;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

@Name("Faction")
public class Faction {
    private static final HashMap<UUID, Faction> STORE = Database.load(Faction.class, Faction::getID);

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

    @Field("Admin")
    private boolean admin;

    @Field("Home")
    private Home home;

    @Field("Invites")
    public ArrayList<String> invites = new ArrayList<String>();

    @Field("Relationships")
    private ArrayList<Relationship> relationships = new ArrayList<Relationship>();

    @Field("relationsLastUpdate")
    public long relationsLastUpdate;

    public Faction(String name, String description, String motd, Formatting color, boolean open, int power, boolean admin, long relationsLastUpdate) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.motd = motd;
        this.description = description;
        this.color = color.getName();
        this.open = open;
        this.admin = admin;
        this.power = admin ? Integer.MAX_VALUE : power;
        Safe safe = new Safe(name);
        Safe.add(safe);
        this.relationsLastUpdate = relationsLastUpdate;
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

    public SimpleInventory getSafe() {
        return Safe.getSafe(name).inventory;
    }

    public void setSafe(SimpleInventory safe) {
        Safe.getSafe(name).inventory = safe;
    }

    public boolean isOpen() {
        return open;
    }

    public void setName(String name) {
        this.name = name;
        FactionEvents.MODIFY.invoker().onModify(this);
    }

    public boolean isAdmin(){
        return admin;
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

    public void setAdmin(boolean admin){
        this.admin = admin;
    }

    public int adjustPower(int adjustment) {
        int maxPower = FactionsMod.CONFIG.MAX_POWER;
        int newPower = Math.min(power + adjustment, maxPower);
        int oldPower = this.power;

        if (newPower == oldPower) return 0;
        if(admin) return 0;
        power = newPower;
        FactionEvents.POWER_CHANGE.invoker().onPowerChange(this, oldPower);
        if(power < 0) {
            this.remove();
        }
        return newPower - oldPower;
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
        Claim.add(new Claim(x, z, level, id, false, null));
    }

    public void addOutpost(int x, int z, String level, Claim.Outpost outpost) {
        Claim.add(new Claim(x, z, level, id, false, outpost));
    }

    public boolean isInvited(String playerName) {
        return invites.stream().anyMatch(invite -> invite.equals(playerName));
    }

    public Home getHome() {
        return home;
    }

    public Claim.Outpost getHome(int index) {
        Claim origin = this.getClaims().stream().filter(Claim::isOutpost)
                .filter(claim ->
                        claim.outpost.index == index).findFirst().get();
        Claim.Outpost pos = null;
        if(origin != null) pos = origin.outpost;
        pos = pos == null ? new Claim.Outpost((int)home.x>>4, (int)home.z>>4,new BlockPos(home.x, home.y, home.z), 0, home.level) : pos;
        return pos;
    }

    public int homesLength(){
        return (int) this.getClaims().stream().filter(Claim::isOutpost).count()+1;
    }

    public void setHome(Home home) {
        this.home = home;
        FactionEvents.SET_HOME.invoker().onSetHome(this, home);
    }

    public Relationship getRelationship(UUID target) {
        return relationships.stream().filter(rel -> rel.target.equals(target)).findFirst().orElse(new Relationship(target, 0));
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

    public List<Relationship> getEnemiesOf() {
        return relationships.stream().filter(rel -> getReverse(rel).status == Relationship.Status.ENEMY).toList();
    }

    public void removeRelationship(UUID target) {
        relationships = new ArrayList<>(relationships.stream().filter(rel -> !rel.target.equals(target)).toList());
    }

    public void setRelationship(Relationship relationship) {
        if (getRelationship(relationship.target) != null) {
            removeRelationship(relationship.target);
        }
        if (relationship.status != Relationship.Status.NEUTRAL)
            relationships.add(relationship);
    }

    public void remove() {
        FactionsManager.playerManager.getServer().getPlayerManager().broadcast(
                new LiteralText("§eThe §9" + this.name + " §efaction has been disbanded!"), MessageType.CHAT, Util.NIL_UUID
        );
        for (User user : getUsers()) {
            user.leaveFaction();
        }
        removeAllClaims();
        Safe.getSafe(name).remove();
        STORE.remove(id);
        FactionEvents.DISBAND.invoker().onDisband(this);

    }

    public static void save() {
        Database.save(Faction.class, STORE.values().stream().toList());
    }
}