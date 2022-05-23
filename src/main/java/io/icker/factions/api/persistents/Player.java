package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.UUID;

import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import io.icker.factions.database.Persistent;

@Name("Player")
public class Player implements Persistent {
    private static final HashMap<UUID, Player> STORE = Database.load(Player.class, p -> p.getID());

    public enum ChatOption {
        FOCUS,
        FACTION,
        GLOBAL
    }

    @Field("ID")
    private final UUID id;

    @Field("Chat")
    private ChatOption chat;

    @Field("Bypass")
    private boolean bypass;

    @Field("ZoneMessage")
    private boolean zoneMessage;

    public Player(UUID id, ChatOption chat, boolean bypass, boolean zoneMessage) {
        this.id = id;
        this.chat = chat;
        this.bypass = bypass;
        this.zoneMessage = zoneMessage;
    }

    public String getKey() {
        return id.toString();
    }

    public static Player get(UUID id) {
        return STORE.get(id);
    }

    public static void add(Player player) {
        STORE.put(player.id, player);
    }

    public UUID getID() {
        return id;
    }
}