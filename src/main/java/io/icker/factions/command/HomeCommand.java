package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.api.persistents.User;
import io.icker.factions.mixin.CombatTrackerAccessor;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;

public class HomeCommand implements Command {
    private int go(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        if (player == null) return 0;

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        if (faction == null) return 0;

        return execGo(player, user, faction);
    }

    public int execGo(ServerPlayer player, User user, Faction faction) {
        Home home = faction.getHome();

        if (home == null) {
            new Message(Component.translatable("factions.command.home.warp.fail.no_home"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (player.level().getServer() == null) return 0;

        ServerLevel world = WorldUtils.getWorld(home.level);

        if (world == null) {
            new Message(Component.translatable("factions.command.home.warp.fail.no_world"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (checkLimitToClaim(faction, world, BlockPos.containing(home.x, home.y, home.z))) {
            new Message(Component.translatable("factions.command.home.warp.fail.no_claim"))
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

        int lastDamageTime =
                ((CombatTrackerAccessor) player.getCombatTracker()).getLastDamageTime();

        if (lastDamageTime == 0
                || player.tickCount - lastDamageTime > FactionsMod.CONFIG.HOME.DAMAGE_COOLDOWN) {
            player.teleportTo(
                    world, home.x, home.y, home.z, new HashSet<>(), home.yaw, home.pitch, false);
            user.homeCooldown = Date.from(Instant.now()).getTime();

            new Message(Component.translatable("factions.command.home.warp.success"))
                    .send(player, false);
        } else {
            new Message(Component.translatable("factions.command.home.warp.fail.combat"))
                    .fail()
                    .send(player, false);
        }
        return 1;
    }

    private int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(player).getFaction();

        if (checkLimitToClaim(faction, (ServerLevel) player.level(), player.blockPosition())) {
            new Message(Component.translatable("factions.command.home.fail.no_claim"))
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
                        player.getYHeadRot(),
                        player.getXRot(),
                        player.level().dimension().identifier().toString());

        faction.setHome(home);
        new Message(
                        Component.translatable(
                                "factions.command.home.set.success",
                                home.x,
                                home.y,
                                home.z,
                                player.getName().getString()))
                .send(faction);
        return 1;
    }

    private static boolean checkLimitToClaim(Faction faction, ServerLevel world, BlockPos pos) {
        if (!FactionsMod.CONFIG.HOME.CLAIM_ONLY) return false;

        ChunkPos chunkPos = world.getChunk(pos).getPos();
        String dimension = world.dimension().identifier().toString();

        Claim possibleClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);
        return possibleClaim == null || possibleClaim.getFaction().getID() != faction.getID();
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("home")
                .requires(
                        Requires.multiple(
                                Requires.isMember(),
                                s -> FactionsMod.CONFIG.HOME != null,
                                Requires.hasPerms("factions.home", 0)))
                .executes(this::go)
                .then(
                        Commands.literal("set")
                                .requires(
                                        Requires.multiple(
                                                Requires.hasPerms("factions.home.set", 0),
                                                Requires.isLeader()))
                                .executes(this::set))
                .build();
    }
}
