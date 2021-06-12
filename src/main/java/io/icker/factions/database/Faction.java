package io.icker.factions.database;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.util.Formatting;

public class Faction {
    public String name;
    public String description;
    public Formatting color;
    public int power;

    public Faction(String name, String description, Formatting color, int power) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.power = power;
    }

    public ArrayList<Claim> getClaims() {
        return Database.Claims.getMultiple(name);
    }

    public Claim claim(int x, int z, String level) {
        return Database.Claims.add(x, z, level, name);
    }

    public void removeClaim(int x, int z, String level) {
        Database.Claims.remove(x, z, level);
    }
    
    public Member addMember(UUID uuid) {
        return Database.Members.add(uuid, name);
    }

    public void remove() {
        Database.Factions.remove(name);
    }
}