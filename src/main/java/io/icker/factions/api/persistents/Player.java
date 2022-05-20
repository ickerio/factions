package io.icker.factions.api.persistents;

import java.util.UUID;

public class Player {
    public enum ChatOption {
        FOCUS,
        FACTION,
        GLOBAL
    }

    public UUID uuid;
    public ChatOption chat;
    public boolean bypass;
    public boolean currentZoneMessage;

    public static Player get(UUID uuid) {
        Query query = new Query("SELECT * FROM PlayerConfig WHERE uuid = ?;")
                .set(uuid)
                .executeQuery();

        if (!query.success) return new Player(uuid, ChatOption.GLOBAL, false, false);

        ChatOption opt;
        try {
            opt = Enum.valueOf(ChatOption.class, query.getString("chat"));
        } catch (IllegalArgumentException e) {
            opt = ChatOption.GLOBAL;
        }

        return new Player(uuid, opt, query.getBool("bypass"), query.getBool("zone"));
    }

    public Player(UUID uuid, ChatOption chat, boolean bypass, boolean currentZoneMessage) {
        this.uuid = uuid;
        this.chat = chat;
        this.bypass = bypass;
        this.currentZoneMessage = currentZoneMessage;
    }

    public void setChat(ChatOption chat) {
        new Query("MERGE INTO PlayerConfig KEY (uuid) VALUES (?, ?, ?, ?);")
                .set(uuid, chat.toString(), bypass, currentZoneMessage)
                .executeUpdate();
    }

    public void setBypass(boolean bypass) {
        new Query("MERGE INTO PlayerConfig KEY (uuid) VALUES (?, ?, ?, ?);")
                .set(uuid, chat.toString(), bypass, currentZoneMessage)
                .executeUpdate();
    }

    public void setZoneMsg(boolean currentZoneMessage) {
        new Query("MERGE INTO PlayerConfig KEY (uuid) VALUES (?, ?, ?, ?);")
                .set(uuid, chat.toString(), bypass, currentZoneMessage)
                .executeUpdate();
    }
}