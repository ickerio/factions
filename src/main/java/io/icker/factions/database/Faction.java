package io.icker.factions.database;

import java.util.UUID;

import net.minecraft.util.Formatting;

public class Faction {
    public String name;
    public String description;
    public Formatting color;
    public boolean open;
    public int power;

    public Faction(String name, String description, Formatting color, boolean open, int power) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.open = open;
        this.power = power;
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

    public void setOpen(boolean open) {
        new Query("UPDATE Faction SET open = ? WHERE name = ?;")
            .set(open, name);
    }

    public void remove() {
        Database.Factions.remove(name);
    }
}