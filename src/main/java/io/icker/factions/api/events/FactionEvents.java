package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class FactionEvents {
    public static final Event<Create> CREATE = EventFactory.createArrayBacked(Create.class, callbacks -> (faction, user) -> {
        for (Create callback : callbacks) {
            callback.onCreate(faction, user);
        }
    });

    public static final Event<Disband> DISBAND = EventFactory.createArrayBacked(Disband.class, callbacks -> (faction) -> {
        for (Disband callback : callbacks) {
            callback.onDisband(faction);
        }
    });

    public static final Event<MemberJoin> MEMBER_JOIN = EventFactory.createArrayBacked(MemberJoin.class, callbacks -> (faction, user) -> {
        for (MemberJoin callback : callbacks) {
            callback.onMemberJoin(faction, user);
        }
    });

    public static final Event<MemberLeave> MEMBER_LEAVE = EventFactory.createArrayBacked(MemberLeave.class, callbacks -> (faction, user) -> {
        for (MemberLeave callback : callbacks) {
            callback.onMemberLeave(faction, user);
        }
    });

    public static final Event<Modify> MODIFY = EventFactory.createArrayBacked(Modify.class, callbacks -> (faction) -> {
        for (Modify callback : callbacks) {
            callback.onModify(faction);
        }
    });

    public static final Event<PowerChange> POWER_CHANGE = EventFactory.createArrayBacked(PowerChange.class, callbacks -> (faction, oldPower) -> {
        for (PowerChange callback : callbacks) {
            callback.onPowerChange(faction, oldPower);
        }
    });

    public static final Event<SetHome> SET_HOME = EventFactory.createArrayBacked(SetHome.class, callbacks -> (faction) -> {
        for (SetHome callback : callbacks) {
            callback.onSetHome(faction);
        }
    });

    public static final Event<RemoveAllClaims> REMOVE_ALL_CLAIMS = EventFactory.createArrayBacked(RemoveAllClaims.class, callbacks -> (faction) -> {
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

    // TODO add Reason: PowerTick, Death
    @FunctionalInterface
    public interface PowerChange {
		void onPowerChange(Faction faction, int oldPower);
	}

    @FunctionalInterface
    public interface SetHome {
		void onSetHome(Faction faction);
	}

    @FunctionalInterface
    public interface RemoveAllClaims {
		void onRemoveAllClaims(Faction faction);
	}
}
