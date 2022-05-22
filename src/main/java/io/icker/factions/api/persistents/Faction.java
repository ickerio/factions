package io.icker.factions.api.persistents;

import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.icker.factions.api.events.PowerChangeEvent;
import io.icker.factions.api.events.RemoveAllClaimsEvent;
import io.icker.factions.api.events.RemoveFactionEvent;
import io.icker.factions.api.events.UpdateFactionEvent;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

@Name("Faction")
public class Faction implements Persistent {
    private static final HashMap<UUID, Faction> STORE = new HashMap<UUID, Faction>();

    @Field("ID")
    private UUID id;

    @Field("Name")
    private String name;

    @Field("Description")
    private String description;

    @Field("Color")
    private String color;

    @Field("Open")
    private boolean open;

    @Field("Power")
    private int power;

    public Faction(String name, String description, String color, boolean open, int power) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.color = color;
        this.open = open;
        this.power = power;
    }

    public String getKey() {
        return id.toString();
    }

    public static Faction get(UUID id) {
        return STORE.get(id);
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
            .collect(Collectors.toList());
    }

    public void setName(String name) {
        this.name = name;
        UpdateFactionEvent.run(this);
    }

    public void setDescription(String description) {
        this.description = description;
        UpdateFactionEvent.run(this);
    }

    public void setColor(Formatting color) {
        this.color = color.getName();
        UpdateFactionEvent.run(this);
    }

    public void setOpen(boolean open) {
        this.open = open;
        UpdateFactionEvent.run(this);
    }

    public void setPower(int power) {
        this.power = power;
        UpdateFactionEvent.run(this);
        PowerChangeEvent.run(this);
    }

    public List<Member> getMembers() {
        return Member.getByFaction(id);
    }

    public void addMember(UUID playerID, Member.Rank rank) {
        Member.add(new Member(playerID, id, rank));
    }

    public List<Claim> getClaims() {
        return Claim.getByFaction(id);
    }

    public void removeAllClaims() {
        // TODO
    }

    public void addClaim(int x, int z, String level) {
        Claim.add(new Claim(x, z, level, id));
    }

    public List<Invite> getInvites() {
        return Invite.getByFaction(id);
    }

    public void remove() {
        STORE.remove(id);
        RemoveFactionEvent.run(this);
    }
}