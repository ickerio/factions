package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.icker.factions.api.events.MutualRelationshipEvent;
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

    public Relationship() { ; }

    public String getKey() {
        return source.toString() + "-" + target.toString();   
    }

    public static Relationship get(UUID source, UUID target) {
        return STORE.getOrDefault(source.toString() + "-" + target.toString(), new Relationship(source, target, Status.NEUTRAL));
    }

    public static List<Relationship> getByFaction(UUID factionID) {
        return STORE.values()
                .stream()
                .filter(i -> i.source == factionID)
                .collect(Collectors.toList());
    }

    public static void set(Relationship relationship) {
        STORE.put(relationship.getKey(), relationship);

        if (relationship.status == Status.NEUTRAL) {
            MutualRelationshipEvent.run(relationship);
        } else if (relationship.getReverse().status == relationship.status) {
            MutualRelationshipEvent.run(relationship);
        }
    }

    public void remove() {
        STORE.remove(getKey());
    }

    public Relationship getReverse() {
        return get(target, source);
    }

    public boolean mutuallyAllies() {
        return status == Status.ALLY && status == getReverse().status;
    }

    public static void save() {
        Database.save(Relationship.class, STORE.values().stream().toList());
    }
}