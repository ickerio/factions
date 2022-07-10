package io.icker.factions.core;

import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static io.icker.factions.FactionsMod.CONFIG;
import static java.lang.String.format;

public class FactionsManager {
    //region Constants
    public static final String
            POWER_LOST_MESSAGE = "%s lost %d power from dying",
            POWER_GAINED_MESSAGE = "%s gained %d power from surviving";
    public static PlayerManager playerManager;
    //endregion

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(FactionsManager::serverStarted);
        FactionEvents.MODIFY.register(FactionsManager::factionModified);
        FactionEvents.MEMBER_JOIN.register(FactionsManager::memberChange);
        FactionEvents.MEMBER_LEAVE.register(FactionsManager::memberChange);
        PlayerEvents.ON_KILLED_BY_PLAYER.register(FactionsManager::playerDeath);
        PlayerEvents.ON_POWER_TICK.register(FactionsManager::powerTick);
        PlayerEvents.OPEN_SAFE.register(FactionsManager::openSafe);
    }

    private static void serverStarted(MinecraftServer server) {
        playerManager = server.getPlayerManager();
        Message.manager = server.getPlayerManager();
    }

    private static void factionModified(Faction faction) {
        ServerPlayerEntity[] players = faction.getUsers()
                .stream()
                .map(user -> playerManager.getPlayer(user.getID()))
                .filter(Objects::nonNull)
                .toArray(ServerPlayerEntity[]::new);
        updatePlayerList(players);
    }

    private static void memberChange(Faction faction, User user) {
        ServerPlayerEntity player = playerManager.getPlayer(user.getID());
        if (player != null) {
            updatePlayerList(player);
        }
    }

    private static void playerDeath(@NotNull ServerPlayerEntity player, DamageSource source) {
        User member = User.get(player.getUuid());
        int adjusted = member.addPower(-CONFIG.POWER.POWER.DEATH_PENALTY);

        final MutableText message = Text.literal(format(POWER_LOST_MESSAGE, player.getName().getString(), adjusted));

        if (member.isInFaction()) {
            final Faction faction = member.getFaction();
            new Message(message).send(faction);
        } else {
            player.sendMessage(message);
        }
    }

    private static void powerTick(@NotNull ServerPlayerEntity player) {
        User member = User.get(player.getUuid());
        if (member.getPower() == member.getMaxPower()) return;
        int adjusted = member.addPower(CONFIG.POWER.POWER_TICKS.REWARD);

        final MutableText message = Text.literal(format(POWER_GAINED_MESSAGE, player.getName().getString(), adjusted));

        if (member.isInFaction()) {
            final Faction faction = member.getFaction();
            new Message(message).send(faction);
        } else {
            player.sendMessage(message);
        }
    }

    private static void updatePlayerList(ServerPlayerEntity... players) {
        playerManager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, players));
    }

    private static ActionResult openSafe(PlayerEntity player, Faction faction) {
        User user = User.get(player.getUuid());

        if (!user.isInFaction()) {
            if (CONFIG.SAFE != null && CONFIG.SAFE.ENDER_CHEST) {
                new Message("Cannot use enderchests when not in a faction").fail().send(player, false);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        }

        player.openHandledScreen(
                new SimpleNamedScreenHandlerFactory(
                        (syncId, inventory, p) -> {
                            if (CONFIG.SAFE.DOUBLE) {
                                return GenericContainerScreenHandler.createGeneric9x6(syncId, inventory, faction.getSafe());
                            } else {
                                return GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, faction.getSafe());
                            }
                        },
                        Text.of(format("%s's Safe", faction.getName()))
                )
        );

        return ActionResult.SUCCESS;
    }
}
