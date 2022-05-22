package io.icker.factions.api.persistents;

import java.util.HashMap;
import java.util.UUID;

import io.icker.factions.database.Field;
import io.icker.factions.database.Name;

@Name("Player")
public class Player {
    private static final HashMap<UUID, Player> STORE = new HashMap<UUID, Player>();

    public enum ChatOption {
        FOCUS,
        FACTION,
        GLOBAL
    }

    @Field("ID")
    public UUID id;

    @Field("Chat")
    public ChatOption chat;

    @Field("Bypass")
    public boolean bypass;

    @Field("ZoneMessage")
    public boolean zoneMessage;

    public Player(UUID id, ChatOption chat, boolean bypass, boolean zoneMessage) {
        this.id = id;
        this.chat = chat;
        this.bypass = bypass;
        this.zoneMessage = zoneMessage;
    }

    public static Player get(UUID id) {
        return STORE.get(id);
    }

    public static void add(Player player) {
        STORE.put(player.id, player);
    }
}