package io.icker.factions;

import io.icker.factions.api.AddMemberEvent;
import io.icker.factions.api.RemoveMemberEvent;
import io.icker.factions.api.UpdateFactionEvent;
import io.icker.factions.command.CommandRegistry;
import io.icker.factions.config.Config;
import io.icker.factions.event.FactionEvents;
import io.icker.factions.event.ServerEvents;
import io.icker.factions.util.DynmapWrapper;
import io.icker.factions.util.PermissionsWrapper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
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
        LOGGER.info("Initialized Factions Mod for Minecraft v1.18");
        try {
            Config.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (PermissionsWrapper.exists()) {
            LOGGER.info("Permissions Mod was found");
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommandRegistry.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerEvents.started(server);
            playerManager = server.getPlayerManager();
            if (FabricLoader.getInstance().isModLoaded("dynmap")) {
                dynmap = new DynmapWrapper();
            } else {
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
