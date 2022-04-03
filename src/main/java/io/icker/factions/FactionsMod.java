package io.icker.factions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.icker.factions.config.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import io.icker.factions.command.CommandRegistry;
import io.icker.factions.command.CommandRegistryWithPermissions;

public class FactionsMod implements ModInitializer {
	public static Logger LOGGER = LogManager.getLogger("Factions");

	@Override
	public void onInitialize() {
		LOGGER.info("Initalized Factions Mod for Minecraft v1.17");
		Config.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			try {
				CommandRegistryWithPermissions.register(dispatcher);
			} catch (java.lang.NoClassDefFoundError e) {
				FactionsMod.LOGGER.info("Fabric Permissions not found");
				CommandRegistry.register(dispatcher);
			}
		});
	}
}
