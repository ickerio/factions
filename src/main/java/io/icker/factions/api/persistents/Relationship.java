package io.icker.factions.api.persistents;

import java.util.UUID;

import io.icker.factions.database.Field;

public class Relationship {
    public enum Status {
        ALLY,
        NEUTRAL,
        ENEMY,
    }

    @Field("Target")
    public UUID target;

    @Field("Status")
    public Status status;

    public Relationship(UUID target, Status status) {
        this.target = target;
        this.status = status;
    }

    public Relationship() { ; }
}