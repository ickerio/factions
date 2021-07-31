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
    public boolean bypass;

    public static PlayerConfig get(UUID uuid) {
        Query query = new Query("SELECT * FROM PlayerConfig WHERE uuid = ?;")
            .set(uuid)
            .executeQuery();

        if (!query.success) return new PlayerConfig(uuid, ChatOption.GLOBAL, false);

        ChatOption opt;
        try {
            opt = Enum.valueOf(ChatOption.class, query.getString("chat"));
        } catch (IllegalArgumentException e) {
            opt = ChatOption.GLOBAL;
        }

        return new PlayerConfig(uuid, opt, query.getBool("bypass"));
    }

    public PlayerConfig(UUID uuid, ChatOption chat, boolean bypass) {
        this.uuid = uuid;
        this.chat = chat;
        this.bypass = bypass;
    }

    public void setChat(ChatOption chat) {
        new Query("MERGE INTO PlayerConfig KEY (uuid) VALUES (?, ?, ?);")
            .set(uuid, chat.toString(), bypass)
            .executeUpdate();
    }

    public void setBypass(boolean bypass) {
        new Query("MERGE INTO PlayerConfig KEY (uuid) VALUES (?, ?, ?);")
            .set(uuid, chat.toString(), bypass)
            .executeUpdate();
    }
}