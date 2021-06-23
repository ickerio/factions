package io.icker.factions.command;


import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Home;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

public class HomeCommand {
    public static int go(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Home home = Member.get(player.getUuid()).getFaction().getHome();

        if (home == null) {
            new Message("No faction home set").fail().send(player, false);
            return 0;
        }

        DamageTracker tracker = player.getDamageTracker();
        if (tracker.getMostRecentDamage() == null || tracker.getTimeSinceLastAttack() > Config.SAFE_TICKS_TO_WARP) {
            player.teleport(
                player.getServer().getWorld(RegistryKey.of(Registry.WORLD_KEY, new Identifier(home.level))),
                home.x, home.y, home.z,
                home.yaw, home.pitch
            );
            new Message("Warped to faction home").send(player, false);
        } else {
            new Message("Unable to warp while in combat").fail().send(player, false);
        }
        return 1;
    }

    public static int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Member.get(player.getUuid()).getFaction();
        Home home = faction.setHome(
            player.getX(), player.getY(), player.getZ(),
            player.getHeadYaw(), player.getPitch(),
            player.getServerWorld().getRegistryKey().getValue().toString()
        );

        new Message("%s set home to %.2f, %.2f, %.2f", player.getName().asString(), home.x, home.y, home.z).send(faction);
        return 1;
    }
}
