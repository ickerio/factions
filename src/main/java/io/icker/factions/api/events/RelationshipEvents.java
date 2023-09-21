package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Relationship;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * All events related to relationships
 */
public final class RelationshipEvents {
    /**
     * When a faction is declared as a different status
     */
    public static final Event<NewDecleration> NEW_DECLARATION =
            EventFactory.createArrayBacked(NewDecleration.class, callbacks -> (relationship) -> {
                for (NewDecleration callback : callbacks) {
                    callback.onNewDecleration(relationship);
                }
            });

    /**
     * When two factions are declared to have the same status
     *
     * For example, mutual allies
     */
    public static final Event<NewMutual> NEW_MUTUAL =
            EventFactory.createArrayBacked(NewMutual.class, callbacks -> (relationship) -> {
                for (NewMutual callback : callbacks) {
                    callback.onNewMutual(relationship);
                }
            });

    /**
     * When a mutual relationship is ended by either of the two factions
     */
    public static final Event<EndMutual> END_MUTUAL = EventFactory
            .createArrayBacked(EndMutual.class, callbacks -> (relationship, oldStatus) -> {
                for (EndMutual callback : callbacks) {
                    callback.onEndMutual(relationship, oldStatus);
                }
            });

    @FunctionalInterface
    public interface NewDecleration {
        void onNewDecleration(Relationship relationship);
    }

    @FunctionalInterface
    public interface NewMutual {
        void onNewMutual(Relationship relationship);
    }

    @FunctionalInterface
    public interface EndMutual {
        void onEndMutual(Relationship relationship, Relationship.Status oldStatus);
    }
}
