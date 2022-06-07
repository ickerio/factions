package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.icker.factions.api.events.RelationshipEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

public class Relationship implements Persistent {
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

    public String getKey() {
        return source.toString() + "-" + target.toString();   
    }

    public Relationship getReverse() {
        return Faction.get(target).getRelationship(source);
    }

    public boolean mutuallyAllies() {
        return status == Status.ALLY && status == getReverse().status;
    }
}