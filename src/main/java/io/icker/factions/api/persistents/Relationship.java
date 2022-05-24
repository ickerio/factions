package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.UUID;

import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

@Name("Relationship")
public class Relationship implements Persistent {
    private static final HashMap<String, Relationship> STORE = Database.load(Relationship.class, c -> c.getKey());

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

    public String getKey() {
        return source.toString() + "-" + target.toString();   
    }

    public static Relationship get(UUID source, UUID target) {
        return STORE.getOrDefault(source.toString() + "-" + target.toString(), new Relationship(source, target, Status.NEUTRAL));
    }

    public static void set(Relationship relationship) {
        STORE.put(relationship.getKey(), relationship);
    }

    public Relationship getReverse() {
        return get(target, source);
    }
}