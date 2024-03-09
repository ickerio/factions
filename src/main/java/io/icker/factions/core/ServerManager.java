package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Safe;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ServerManager {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(ServerManager::playerJoin);
        MiscEvents.ON_SAVE.register(ServerManager::save);
        ServerLifecycleEvents.SERVER_STARTED.register(ServerManager::tax);
    }

    private static void tax(MinecraftServer server) {
        Date date = new Date();
        date.setHours(FactionsMod.CONFIG.TAXES_HOURS);
        date.setMinutes(FactionsMod.CONFIG.TAXES_MINUTES);

        Date newDate = new Date().after(date) ? DateUtils.addDays(date, 1) : date;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Date date = new Date();
                server.getPlayerManager().broadcast(new LiteralText("§9It's " + date.toString() + ";"), MessageType.CHAT, Util.NIL_UUID);
                server.getPlayerManager().broadcast(new LiteralText("§9Time to collect some taxes"), MessageType.CHAT, Util.NIL_UUID);
                Faction.all().forEach(faction -> {
                    faction.adjustPower(-(1 + faction.getClaims().size()* FactionsMod.CONFIG.DAILY_TAX_PER_CHUNK));
                });
            }
        }, newDate);
    }

    private static void save(MinecraftServer server) {
        Claim.save();
        Faction.save();
        User.save();
        Safe.save();
        Safe.saveBackup();
    }

    private static void playerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        User user = User.get(player.getName().getString());

        if (user.isInFaction()) {
            Faction faction = user.getFaction();
            new Message("Welcome back " + player.getName().getString() + "!").send(player, false);
            new Message(faction.getMOTD()).prependFaction(faction).send(player, false);
        }
    }
}