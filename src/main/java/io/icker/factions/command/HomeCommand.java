package io.icker.factions.command;


import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.config.Config;
import io.icker.factions.database.Home;
import io.icker.factions.database.Member;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

public class HomeCommand {
    public static int go(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Home home = Member.get(player.getUuid()).getFaction().getHome();

        if (home == null) {
            source.sendFeedback(new LiteralText("No faction home set").formatted(Formatting.RED), false);
            return 0;
        }

        DamageTracker tracker = player.getDamageTracker();
        if (tracker.getMostRecentDamage() == null || tracker.getTimeSinceLastAttack() > Config.SAFE_TICKS_TO_WARP) {
            player.teleport(
                player.getServer().getWorld(RegistryKey.of(Registry.WORLD_KEY, new Identifier(home.level))),
                home.x, home.y, home.z,
                home.yaw, home.pitch
            );
            source.sendFeedback(new LiteralText("Warped to faction home"), false);
        } else {
            source.sendFeedback(new LiteralText("Unable to warp while in combat").formatted(Formatting.RED), false);
        }
        return 1;
    }

    public static int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Home home = Member.get(player.getUuid()).getFaction()
            .setHome(
                player.getX(), player.getY(), player.getZ(),
                player.getHeadYaw(), player.getPitch(),
                player.getServerWorld().getRegistryKey().getValue().toString()
            );

        source.sendFeedback(new LiteralText(String.format("Set faction home to %.2f, %.2f, %.2f", home.x, home.y, home.z)), false);
        return 1;
    }
}