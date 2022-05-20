package io.icker.factions;

import io.icker.factions.api.events.AddMemberEvent;
import io.icker.factions.api.events.RemoveMemberEvent;
import io.icker.factions.api.events.UpdateFactionEvent;
import io.icker.factions.command.CommandRegistry;
import io.icker.factions.config.Config;
import io.icker.factions.database.Database;
import io.icker.factions.database.TestPersistent;
import io.icker.factions.event.FactionEvents;
import io.icker.factions.event.ServerEvents;
import io.icker.factions.util.DynmapWrapper;
import io.icker.factions.util.PermissionsWrapper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class FactionsMod implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger("Factions");
    public static DynmapWrapper dynmap;
    public static PlayerManager playerManager;


    @Override
    public void onInitialize() {
        Database.setup(TestPersistent.class);

        List<TestPersistent> items = Database.load(TestPersistent.class);
        //TestPersistent a = new TestPersistent("David");
       // a.setNumber(69);
        //items.add(a);
       // Database.save(TestPersistent.class, items);

       for (TestPersistent item : items) {
           LOGGER.info(item.getName(), item.getNumber());
       }



        LOGGER.info("Initialized Factions Mod for Minecraft v1.18");
        try {
            Config.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (PermissionsWrapper.exists()) {
            LOGGER.info("Permissions Mod was found");
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            CommandRegistry.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerEvents.started(server);
            playerManager = server.getPlayerManager();
            try {
                dynmap = new DynmapWrapper();
            } catch (java.lang.NoClassDefFoundError e) {
                LOGGER.info("Dynmap not found");
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerEvents.stopped(server);
        });

        UpdateFactionEvent.register((faction) -> {
            List<ServerPlayerEntity> players = faction.getMembers().stream().map(member -> FactionsMod.playerManager.getPlayer(member.uuid)).toList();
            FactionEvents.updatePlayerList(players);
        });

        AddMemberEvent.register((member) -> {
            FactionEvents.updatePlayerList(FactionsMod.playerManager.getPlayer(member.uuid));
        });

        RemoveMemberEvent.register((member) -> {
            FactionEvents.updatePlayerList(FactionsMod.playerManager.getPlayer(member.uuid));
        });
    }
}
