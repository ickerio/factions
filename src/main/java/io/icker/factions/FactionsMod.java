package io.icker.factions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.command.AdminCommand;
import io.icker.factions.command.ClaimCommand;
import io.icker.factions.command.CreateCommand;
import io.icker.factions.command.DeclareCommand;
import io.icker.factions.command.DisbandCommand;
import io.icker.factions.command.HomeCommand;
import io.icker.factions.command.InfoCommand;
import io.icker.factions.command.InviteCommand;
import io.icker.factions.command.JoinCommand;
import io.icker.factions.command.KickCommand;
import io.icker.factions.command.LeaveCommand;
import io.icker.factions.command.ListCommand;
import io.icker.factions.command.MapCommand;
import io.icker.factions.command.MemberCommand;
import io.icker.factions.command.ModifyCommand;
import io.icker.factions.command.PermissionCommand;
import io.icker.factions.command.RankCommand;
import io.icker.factions.command.SafeCommand;
import io.icker.factions.command.SettingsCommand;
import io.icker.factions.config.Config;
import io.icker.factions.core.ChatManager;
import io.icker.factions.core.FactionsManager;
import io.icker.factions.core.InteractionManager;
import io.icker.factions.core.ServerManager;
import io.icker.factions.core.SoundManager;
import io.icker.factions.core.WorldManager;
import io.icker.factions.util.Command;
import io.icker.factions.util.DynmapWrapper;
import io.icker.factions.util.PlaceholdersWrapper;
import io.icker.factions.util.WorldUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class FactionsMod implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger("Factions");
    public static final String MODID = "factions";

    public static Config CONFIG = Config.load();
    public static DynmapWrapper dynmap;

    @Override
    public void onInitialize() {
        LOGGER.info("Initialized Factions Mod for Minecraft v1.20.1");

        WorldUtils.register();

        dynmap = FabricLoader.getInstance().isModLoaded("dynmap") ? new DynmapWrapper() : null;
        if (FabricLoader.getInstance().isModLoaded("placeholder-api"))
            PlaceholdersWrapper.init();

        ChatManager.register();
        FactionsManager.register();
        InteractionManager.register();
        ServerManager.register();
        SoundManager.register();
        WorldManager.register();

        CommandRegistrationCallback.EVENT.register(FactionsMod::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        LiteralCommandNode<ServerCommandSource> factions =
                CommandManager.literal("factions").build();

        LiteralCommandNode<ServerCommandSource> alias = CommandManager.literal("f").build();

        dispatcher.getRoot().addChild(factions);
        dispatcher.getRoot().addChild(alias);

        Command[] commands = new Command[] {new AdminCommand(), new SettingsCommand(),
                new ClaimCommand(), new CreateCommand(), new DeclareCommand(), new DisbandCommand(),
                new HomeCommand(), new InfoCommand(), new InviteCommand(), new JoinCommand(),
                new KickCommand(), new LeaveCommand(), new ListCommand(), new MapCommand(),
                new MemberCommand(), new ModifyCommand(), new RankCommand(), new SafeCommand(),
                new PermissionCommand()};

        for (Command command : commands) {
            factions.addChild(command.getNode());
            alias.addChild(command.getNode());
        }
    }
}
