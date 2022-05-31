package io.icker.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.command.*;
import io.icker.factions.config.Config;
import io.icker.factions.core.FactionsManager;
import io.icker.factions.core.ServerEvents;
import io.icker.factions.util.Command;
import io.icker.factions.util.DynmapWrapper;
import io.icker.factions.util.Migrator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class FactionsMod implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger("Factions");
    
    public static Config CONFIG;
    public static DynmapWrapper dynmap;

    @Override
    public void onInitialize() {
        LOGGER.info("Initialized Factions Mod for Minecraft v1.18");
        CONFIG = Config.load();

        dynmap = FabricLoader.getInstance().isModLoaded("dynmap") ? new DynmapWrapper() : null;
        Migrator.migrate();

        CommandRegistrationCallback.EVENT.register(FactionsMod::registerCommands);
        ServerPlayConnectionEvents.JOIN.register(ServerEvents::playerJoin);
        ServerLifecycleEvents.SERVER_STARTED.register(FactionsManager::serverStarted);
        FactionEvents.MODIFY.register(FactionsManager::factionModified);
        FactionEvents.MEMBER_JOIN.register(FactionsManager::memberChange);
        FactionEvents.MEMBER_LEAVE.register(FactionsManager::memberChange);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
		LiteralCommandNode<ServerCommandSource> factions = CommandManager
			.literal("factions")
			.build();

		LiteralCommandNode<ServerCommandSource> alias = CommandManager
			.literal("f")
			.redirect(factions)
			.build();

		dispatcher.getRoot().addChild(factions);
		dispatcher.getRoot().addChild(alias);

		Command[] commands = new Command[] {
            new AdminCommand(),
			new ChatCommand(),
            new ClaimCommand(),
            new CreateCommand(),
            new DeclareCommand(),
            new DisbandCommand(),
            new HomeCommand(),
            new InfoCommand(),
            new InviteCommand(),
            new JoinCommand(),
            new KickCommand(),
            new LeaveCommand(),
            new ListCommand(),
            new MapCommand(),
            new ModifyCommand(),
            new Radar(),
            new RankCommand(),
		};

		for (Command command : commands) {
			factions.addChild(command.getNode());
		}
    }
}
