package io.icker.factions.teams;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.util.Formatting;

public class Team {
    public String name;
    public String description;
    public Formatting color;
    public int power;

    public Team(String name, String description, Formatting color, int power) {
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

    
    public void unclaim(int x, int z, String level) {
        Database.Claims.remove(x, z, level, name);
    }
    
    public Member addMember(UUID uuid) {
        return Database.Members.add(uuid, name);
    }
}