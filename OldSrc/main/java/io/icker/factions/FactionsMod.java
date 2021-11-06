package io.icker.factions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.icker.factions.config.Config;
import net.fabricmc.api.ModInitializer;

public class FactionsMod implements ModInitializer {
	public static Logger LOGGER = LogManager.getLogger("Factions");

	@Override
	public void onInitialize() {
		LOGGER.info("Initalized Factions Mod for Minecraft v1.17");
		Config.init();
	}
}
