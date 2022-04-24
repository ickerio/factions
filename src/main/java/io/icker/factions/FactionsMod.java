package io.icker.factions;

import net.minecraft.server.PlayerManager;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.icker.factions.config.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import io.icker.factions.command.CommandRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import io.icker.factions.event.ServerEvents;
import io.icker.factions.util.DynmapWrapper;
import io.icker.factions.util.PermissionsWrapper;

public class FactionsMod implements ModInitializer {
	public static Logger LOGGER = LogManager.getLogger("Factions");
	public static DynmapWrapper dynmap;
	public static boolean dynmapEnabled;
	public static PlayerManager playerManager;

	@Override
	public void onInitialize() {
		LOGGER.info("Initalized Factions Mod for Minecraft v1.18");
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
				dynmapEnabled = true;
			} catch (java.lang.NoClassDefFoundError e) {
				LOGGER.info("Dynmap not found");
				dynmapEnabled = false;
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ServerEvents.stopped(server);
		});
	}
}
