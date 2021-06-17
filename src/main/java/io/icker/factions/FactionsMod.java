package io.icker.factions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.icker.factions.command.CommandRegister;
import io.icker.factions.event.PlayerInteractEvents;
import io.icker.factions.event.ServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

public class FactionsMod implements ModInitializer {
	public static Logger LOGGER = LogManager.getLogger("factions");

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(ServerEvents::starting);
		ServerLifecycleEvents.SERVER_STOPPED.register(ServerEvents::stopped);
		ServerTickEvents.END_SERVER_TICK.register(ServerEvents::tick);
		PlayerBlockBreakEvents.BEFORE.register(PlayerInteractEvents::breakBlocks);
		UseBlockCallback.EVENT.register(PlayerInteractEvents::useBlocks);
		UseItemCallback.EVENT.register(PlayerInteractEvents::useItem);
		CommandRegistrationCallback.EVENT.register(CommandRegister::register);
	}
}
