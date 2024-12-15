package io.icker.factions.api.persistents;

import io.icker.factions.FactionsMod;
import io.icker.factions.database.Field;

import java.util.ArrayList;
import java.util.UUID;

public class Relationship {
    public enum Status {
        ALLY,
        NEUTRAL,
        ENEMY,
    }

    public enum Permissions {
        USE_BLOCKS,
        PLACE_BLOCKS,
        BREAK_BLOCKS,
        USE_ENTITIES,
        ATTACK_ENTITIES,
        USE_INVENTORIES
    }

    @Field("Target")
    public UUID target;

    @Field("Status")
    public Status status;

    @Field("Permissions")
    public ArrayList<Permissions> permissions =
            new ArrayList<>(FactionsMod.CONFIG.RELATIONSHIPS.DEFAULT_GUEST_PERMISSIONS);

    public Relationship(UUID target, Status status) {
        this.target = target;
        this.status = status;
    }

    public Relationship() {}
}
