package io.icker.factions.teams;

import java.util.UUID;

public class Member {
    public UUID uuid;
    public transient Team team;
    public int addPowerTick = 12000; // 10 mins at 20tps

    public Member(UUID uuid, Team team) {
        this.uuid = uuid;
        this.team = team;
    }

    public void tick() {
        addPowerTick -= 1;
        if (addPowerTick == 0) {
            addPowerTick = 1200;
            team.power++;
        }
    }
}