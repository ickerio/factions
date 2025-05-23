package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.api.persistents.User;
import io.icker.factions.mixin.DamageTrackerAccessor;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;

public class HomeCommand implements Command {
    private int go(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (player == null) return 0;

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        if (faction == null) return 0;

        return execGo(player, user, faction);
    }

    public int execGo(ServerPlayerEntity player, User user, Faction faction) {
        Home home = faction.getHome();

        if (home == null) {
            new Message(Text.translatable("factions.command.home.warp.fail.no_home"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (player.getServer() == null) return 0;

        ServerWorld world = WorldUtils.getWorld(home.level);

        if (world == null) {
            new Message(Text.translatable("factions.command.home.warp.fail.no_world"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (checkLimitToClaim(faction, world, BlockPos.ofFloored(home.x, home.y, home.z))) {
            new Message(Text.translatable("factions.command.home.warp.fail.no_claim"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        long elapsed_time = Date.from(Instant.now()).getTime() - user.homeCooldown;
        if (elapsed_time < FactionsMod.CONFIG.HOME.HOME_WARP_COOLDOWN_SECOND * 1000) {
            new Message(
                            "Cannot warp home while on warp cooldown, please wait %.0f seconds",
                            (double)
                                            (FactionsMod.CONFIG.HOME.HOME_WARP_COOLDOWN_SECOND
                                                            * 1000
                                                    - elapsed_time)
                                    / 1000.0)
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (((DamageTrackerAccessor) player.getDamageTracker()).getAgeOnLastDamage() == 0
                || player.age
                                - ((DamageTrackerAccessor) player.getDamageTracker())
                                        .getAgeOnLastDamage()
                        > FactionsMod.CONFIG.HOME.DAMAGE_COOLDOWN) {
            player.teleport(
                    world, home.x, home.y, home.z, new HashSet<>(), home.yaw, home.pitch, false);
            user.homeCooldown = Date.from(Instant.now()).getTime();

            new Message(Text.translatable("factions.command.home.warp.success"))
                    .send(player, false);
        } else {
            new Message(Text.translatable("factions.command.home.warp.fail.combat"))
                    .fail()
                    .send(player, false);
        }
        return 1;
    }

    private int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        Faction faction = Command.getUser(player).getFaction();

        if (checkLimitToClaim(faction, (ServerWorld) player.getWorld(), player.getBlockPos())) {
            new Message(Text.translatable("factions.command.home.fail.no_claim"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        Home home =
                new Home(
                        faction.getID(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getHeadYaw(),
                        player.getPitch(),
                        player.getWorld().getRegistryKey().getValue().toString());

        faction.setHome(home);
        new Message(
                        Text.translatable(
                                "factions.command.home.set.success",
                                home.x,
                                home.y,
                                home.z,
                                player.getName().getString()))
                .send(faction);
        return 1;
    }

    private static boolean checkLimitToClaim(Faction faction, ServerWorld world, BlockPos pos) {
        if (!FactionsMod.CONFIG.HOME.CLAIM_ONLY) return false;

        ChunkPos chunkPos = world.getChunk(pos).getPos();
        String dimension = world.getRegistryKey().getValue().toString();

        Claim possibleClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);
        return possibleClaim == null || possibleClaim.getFaction().getID() != faction.getID();
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("home")
                .requires(
                        Requires.multiple(
                                Requires.isMember(),
                                s -> FactionsMod.CONFIG.HOME != null,
                                Requires.hasPerms("factions.home", 0)))
                .executes(this::go)
                .then(
                        CommandManager.literal("set")
                                .requires(
                                        Requires.multiple(
                                                Requires.hasPerms("factions.home.set", 0),
                                                Requires.isLeader()))
                                .executes(this::set))
                .build();
    }
}
