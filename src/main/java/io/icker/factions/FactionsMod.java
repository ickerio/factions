package io.icker.factions;

import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.command.*;
import io.icker.factions.config.Config;
import io.icker.factions.core.FactionsManager;
import io.icker.factions.core.ServerEvents;
import io.icker.factions.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

public class FactionsMod implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger("Factions");
    
    public static Config CONFIG;
    public static DynmapWrapper dynmap;
    public static PlayerManager playerManager;


    @Override
    public void onInitialize() {
        LOGGER.info("Initialized Factions Mod for Minecraft v1.18");
        CONFIG = Config.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            registerCommands(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            playerManager = server.getPlayerManager();
            if (FabricLoader.getInstance().isModLoaded("dynmap")) {
                dynmap = new DynmapWrapper();
            } else {
                LOGGER.info("Dynmap not found");
            }

            Message.manager = server.getPlayerManager();
            Migrator.migrate();
        });
        

        FactionEvents.MODIFY.register(faction -> {
            List<ServerPlayerEntity> players = faction.getUsers().stream().map(user -> FactionsMod.playerManager.getPlayer(user.getID())).toList();
            players.removeAll(Collections.singletonList(null));
            FactionsManager.updatePlayerList(players);
        });

        FactionEvents.MEMBER_JOIN.register((faction, user) -> {
            FactionsManager.updatePlayerList(FactionsMod.playerManager.getPlayer(user.getID()));
        });

        FactionEvents.MEMBER_LEAVE.register((faction, user) -> {
            FactionsManager.updatePlayerList(FactionsMod.playerManager.getPlayer(user.getID()));
        });

        ServerPlayConnectionEvents.JOIN.register(ServerEvents::playerJoin);
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
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
            new RankCommand(),
            new ZoneMsgCommand()
		};

		for (Command command : commands) {
			factions.addChild(command.getNode());
		}
    }
}
