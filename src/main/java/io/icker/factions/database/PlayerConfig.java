package io.icker.factions.database;

import java.util.UUID;

public class PlayerConfig { 
    public enum ChatOption {
        FOCUS,
        FACTION,
        GLOBAL;
    }

    public UUID uuid;
    public ChatOption chatOption;

    public PlayerConfig(UUID uuid) {
        this.uuid = uuid;
    }

    public ChatOption getChatOption() {
        Query query = new Query("SELECT option FROM PlayerConfig WHERE uuid = ?;")
            .set(uuid)
            .executeQuery();

        if (!query.success) return ChatOption.GLOBAL;

        try {
            return Enum.valueOf(ChatOption.class, query.getString("option"));
        } catch (IllegalArgumentException e) {
            return ChatOption.GLOBAL;
        }
    }

    public void setChatOption(ChatOption option) {
        new Query("MERGE INTO PlayerConfig KEY (uuid) VALUES (?, ?);")
            .set(uuid, option.toString())
            .executeUpdate();
    }
}