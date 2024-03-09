package io.icker.factions.api.persistents;

import java.util.UUID;

import io.icker.factions.FactionsMod;
import io.icker.factions.database.Field;

public class Relationship {
    public enum Status {
        ALLY,
        IMPROVED,
        NEUTRAL,
        INSULTED,
        ENEMY;
    }

    @Field("Target")
    public UUID target;

    @Field("Status")
    public Status status;

    @Field("points")
    public int points;

    public Relationship(UUID target, int points) {
        this.target = target;
        this.points = points > FactionsMod.CONFIG.DAYS_TO_FABRICATE + 1 ? FactionsMod.CONFIG.DAYS_TO_FABRICATE : points;
        this.points = points < -FactionsMod.CONFIG.DAYS_TO_FABRICATE - 1 ? FactionsMod.CONFIG.DAYS_TO_FABRICATE - 1 : points;
        this.status = points == 0 ? Status.NEUTRAL : points < -FactionsMod.CONFIG.DAYS_TO_FABRICATE ? Status.ENEMY : points > FactionsMod.CONFIG.DAYS_TO_FABRICATE ? Status.ALLY : points > 0 ? Status.IMPROVED : Status.INSULTED;
    }

    public Relationship() { ; }
}