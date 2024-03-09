package io.icker.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.command.*;
import io.icker.factions.config.Config;
import io.icker.factions.core.*;
import io.icker.factions.util.Command;
import io.icker.factions.util.DynmapWrapper;
import io.icker.factions.util.Migrator;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class FactionsMod implements DedicatedServerModInitializer{
    public static Logger LOGGER = LogManager.getLogger("Factions");

    public static Config CONFIG = Config.load();
    public static DynmapWrapper dynmap;
    public static LuckPerms LUCK_API;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Initialized Factions Mod for Minecraft v1.18");

        dynmap = FabricLoader.getInstance().isModLoaded("dynmap") ? new DynmapWrapper() : null;
        Migrator.migrate();

        FactionsManager.register();
        InteractionManager.register();
        ServerManager.register();
        SoundManager.register();
        WorldManager.register();


        CommandRegistrationCallback.EVENT.register(FactionsMod::registerCommands);
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
        {LUCK_API = LuckPermsProvider.get();}
        );
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
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
            new SettingsCommand(),
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
            new SafeCommand(),
                new BurnResourcesCommand()
        };

        for (Command command : commands) {
            factions.addChild(command.getNode());
            alias.addChild(command.getNode());
        }
    }
}
