package io.icker.factions.api.persistents;

import java.util.UUID;

import io.icker.factions.database.Field;

public class Relationship {
    public enum Status {
        ALLY,
        NEUTRAL,
        ENEMY,
    }

    @Field("Source")
    public UUID source;

    @Field("Target")
    public UUID target;

    @Field("Status")
    public Status status;

    public Relationship(UUID source, UUID target, Status status) {
        this.source = source;
        this.target = target;
        this.status = status;
    }

    public Relationship() { ; }

    public Relationship getReverse() {
        return Faction.get(target).getRelationship(source);
    }

    public boolean mutuallyAllies() {
        return status == Status.ALLY && status == getReverse().status;
    }
}