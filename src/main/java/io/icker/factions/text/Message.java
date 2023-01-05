package io.icker.factions.text;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;

import java.util.ArrayList;
import java.util.UUID;

public class Message {
    public static PlayerManager manager;
    private final ArrayList<Text> contents = new ArrayList<>();

    public Message append(Text text) {
        contents.add(text);
        return this;
    }

    public Message prepend(Text text) {
        contents.add(0, text);
        return this;
    }

    public Message send(PlayerEntity player, boolean actionBar) {
        player.sendMessage(this.build(player.getUuid()), actionBar);
        return this;
    }

    public Message send(Faction faction) {
        prepend(new FactionText(faction));
        for (User member : faction.getUsers()) {
            ServerPlayerEntity player = manager.getPlayer(member.getID());
            if (player != null) send(player, false);
        }
        return this;
    }

    public MutableText build(UUID userId) {
        MutableText built = (MutableText) net.minecraft.text.Text.of("");

        for (Text text : contents) {
            built.append(text.build(userId));
        }

        return built;
    }
}
