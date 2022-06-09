package io.icker.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.core.ChatManager;
import net.minecraft.command.CommandRegistryAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.icker.factions.command.*;
import io.icker.factions.config.Config;
import io.icker.factions.core.InteractionManager;
import io.icker.factions.core.FactionsManager;
import io.icker.factions.core.ServerManager;
import io.icker.factions.core.WorldManager;
import io.icker.factions.util.Command;
import io.icker.factions.util.DynmapWrapper;
import io.icker.factions.util.Migrator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class FactionsMod implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger("Factions");

    public static Config CONFIG;
    public static DynmapWrapper dynmap;

    @Override
    public void onInitialize() {
        LOGGER.info("Initialized Factions Mod for Minecraft v1.19");
        CONFIG = Config.load();

        ChatManager.register();

        dynmap = FabricLoader.getInstance().isModLoaded("dynmap") ? new DynmapWrapper() : null;
        Migrator.migrate();

        FactionsManager.register();
        InteractionManager.register();
        ServerManager.register();
        WorldManager.register();

        CommandRegistrationCallback.EVENT.register(FactionsMod::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralCommandNode<ServerCommandSource> factions = CommandManager
            .literal("factions")
            .build();

        LiteralCommandNode<ServerCommandSource> alias = CommandManager
            .literal("f")
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
            new RadarCommand(),
            new RankCommand(),
        };

        for (Command command : commands) {
            factions.addChild(command.getNode());
            alias.addChild(command.getNode());
        }
    }
}
