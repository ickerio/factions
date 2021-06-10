package io.icker.factions.teams;

public class Claim {
    public int x;
    public int z;
    public String level;
    public transient Team owner;

    public Claim(int x, int z, String level, Team owner) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.owner = owner;
    }
}
