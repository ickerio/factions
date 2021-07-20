package io.icker.factions.database;

import java.util.UUID;

public class PlayerConfig { 
    public enum ChatOption {
        FOCUS,
        FACTION,
        GLOBAL;
    }

    public UUID uuid;
    public ChatOption chat;

    public static PlayerConfig get(UUID uuid) {
        Query query = new Query("SELECT chat FROM PlayerConfig WHERE uuid = ?;")
            .set(uuid)
            .executeQuery();

        if (!query.success) return new PlayerConfig(uuid, ChatOption.GLOBAL);

        try {
            return new PlayerConfig(uuid, Enum.valueOf(ChatOption.class, query.getString("chat")));
        } catch (IllegalArgumentException e) {
            return new PlayerConfig(uuid, ChatOption.GLOBAL);
        }
    }

    public PlayerConfig(UUID uuid, ChatOption chat) {
        this.uuid = uuid;
        this.chat = chat;
    }

    public void setChat(ChatOption chat) {
        new Query("MERGE INTO PlayerConfig KEY (uuid) VALUES (?, ?);")
            .set(uuid, chat.toString())
            .executeUpdate();
    }
}