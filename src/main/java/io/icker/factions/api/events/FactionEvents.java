package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.api.persistents.User;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Events related to {@link Faction}
 */
public final class FactionEvents {
    /**
     * Called when a {@link Faction} is created
     */
    public static final Event<Create> CREATE =
            EventFactory.createArrayBacked(Create.class, callbacks -> (faction, user) -> {
                for (Create callback : callbacks) {
                    callback.onCreate(faction, user);
                }
            });

    /**
     * Called when a {@link Faction} is disbanded
     */
    public static final Event<Disband> DISBAND =
            EventFactory.createArrayBacked(Disband.class, callbacks -> (faction) -> {
                for (Disband callback : callbacks) {
                    callback.onDisband(faction);
                }
            });

    /**
     * Called when a {@link User} joins a {@link Faction}
     */
    public static final Event<MemberJoin> MEMBER_JOIN =
            EventFactory.createArrayBacked(MemberJoin.class, callbacks -> (faction, user) -> {
                for (MemberJoin callback : callbacks) {
                    callback.onMemberJoin(faction, user);
                }
            });

    /**
     * Called when a {@link User} leaves a {@link Faction}
     */
    public static final Event<MemberLeave> MEMBER_LEAVE =
            EventFactory.createArrayBacked(MemberLeave.class, callbacks -> (faction, user) -> {
                for (MemberLeave callback : callbacks) {
                    callback.onMemberLeave(faction, user);
                }
            });

    /**
     * Called when a factions name, description, MOTD, color or open status is modified
     */
    public static final Event<Modify> MODIFY =
            EventFactory.createArrayBacked(Modify.class, callbacks -> (faction) -> {
                for (Modify callback : callbacks) {
                    callback.onModify(faction);
                }
            });

    /**
     * Called when a factions power changes
     */
    public static final Event<PowerChange> POWER_CHANGE =
            EventFactory.createArrayBacked(PowerChange.class, callbacks -> (faction, oldPower) -> {
                for (PowerChange callback : callbacks) {
                    callback.onPowerChange(faction, oldPower);
                }
            });

    /**
     * Called when a faction sets its {@link Home}
     */
    public static final Event<SetHome> SET_HOME =
            EventFactory.createArrayBacked(SetHome.class, callbacks -> (faction, home) -> {
                for (SetHome callback : callbacks) {
                    callback.onSetHome(faction, home);
                }
            });

    /**
     * Called when a faction removes all its claims. (Note that each claim will also run a
     * {@link ClaimEvents} REMOVE event)
     */
    public static final Event<RemoveAllClaims> REMOVE_ALL_CLAIMS =
            EventFactory.createArrayBacked(RemoveAllClaims.class, callbacks -> (faction) -> {
                for (RemoveAllClaims callback : callbacks) {
                    callback.onRemoveAllClaims(faction);
                }
            });

    @FunctionalInterface
    public interface Create {
        void onCreate(Faction faction, User owner);
    }

    @FunctionalInterface
    public interface Disband {
        void onDisband(Faction faction);
    }

    @FunctionalInterface
    public interface MemberJoin {
        void onMemberJoin(Faction faction, User user);
    }

    // TODO add Reason: LEAVE, KICK, DISBAND
    @FunctionalInterface
    public interface MemberLeave {
        void onMemberLeave(Faction faction, User user);
    }

    @FunctionalInterface
    public interface Modify {
        void onModify(Faction faction);
    }

    @FunctionalInterface
    public interface PowerChange {
        void onPowerChange(Faction faction, int oldPower);
    }

    @FunctionalInterface
    public interface SetHome {
        void onSetHome(Faction faction, Home home);
    }

    @FunctionalInterface
    public interface RemoveAllClaims {
        void onRemoveAllClaims(Faction faction);
    }
}
