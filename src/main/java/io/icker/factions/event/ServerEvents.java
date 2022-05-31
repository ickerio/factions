package io.icker.factions.event;

import io.icker.factions.api.persistents.*;
import io.icker.factions.util.Message;
import io.icker.factions.util.Migrator;
import net.minecraft.server.MinecraftServer;

public class ServerEvents {
    public static void save() {
        Claim.save();
        Faction.save();
        Home.save();
        Invite.save();
        User.save();
        Relationship.save();
    }
}