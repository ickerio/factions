package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.icker.factions.api.events.RelationshipEvents;
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
            .toList();
    }

    public static void set(Relationship relationship) {
        Status oldStatus = STORE.get(relationship.getKey()).status;
        
        STORE.put(relationship.getKey(), relationship);
        RelationshipEvents.NEW_DECLARATION.invoker().onNewDecleration(relationship);

        Status reverseStatus = relationship.getReverse().status;
        if (relationship.status == reverseStatus) {
            RelationshipEvents.NEW_MUTUAL.invoker().onNewMutual(relationship);
        } else if (oldStatus == reverseStatus) {
            RelationshipEvents.END_MUTUAL.invoker().onEndMutual(relationship, oldStatus);
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