package io.icker.factions.teams;

public class Claim {
    public int x;
    public int z;
    public String level;
    private String teamName;

    public Claim(int x, int z, String level, String team) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.teamName = team;
    }

    public Team getTeam() {
        return Database.Teams.get(teamName);
    }
}
