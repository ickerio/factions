package io.icker.factions.teams;

import java.util.UUID;

public class Member {
    public UUID uuid;
    private String teamName;

    public Member(UUID uuid, String team) {
        this.uuid = uuid;
        this.teamName = team;
    }

    public Team getTeam() {
        return Database.Teams.get(teamName);
    }
}