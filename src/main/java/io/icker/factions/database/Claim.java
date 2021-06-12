package io.icker.factions.database;

public class Claim {
    public int x;
    public int z;
    public String level;
    private String factionName;

    public Claim(int x, int z, String level, String faction) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.factionName = faction;
    }

    public Faction getFaction() {
        return Database.Factions.get(factionName);
    }
}
