package io.icker.factions.event;

import io.icker.factions.api.persistents.*;
import io.icker.factions.util.Message;
import net.minecraft.server.MinecraftServer;

public class ServerEvents {
    public static void started(MinecraftServer server) {
        // Do new database setup things here
        Message.manager = server.getPlayerManager();
    }

    public static void save() {
        Claim.save();
        Faction.save();
        Home.save();
        Invite.save();
        Member.save();
        Relationship.save();
    }
}