package io.icker.factions.teams;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.util.Formatting;

public class Team {
    public String name;
    public Formatting color;
    public ArrayList<Member> members;
    public ArrayList<Claim> claims;
    public int power;

    public Team(String name, Formatting color) {
        this.name = name;
        this.color = color;
        this.members = new ArrayList<Member>();
        this.claims = new ArrayList<Claim>();
        this.power = 100;
    }

    public Claim claim(int x, int z, String level) {
        Claim newClaim = new Claim(x, z, level, this);
        claims.add(newClaim);
        return newClaim;
    }

    public boolean unclaim(int x, int z, String level) {
        return claims.removeIf(c -> c.x == x & c.z == z && c.level == level);
    }

    public Member addMember(UUID uuid) {
        Member newMember = new Member(uuid, this);
        members.add(newMember);
        return newMember;
    }
}