package io.icker.factions.command;


import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.Optional;

public class HomeCommand implements Command {
    private int go(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) return 0;

        Faction faction = Command.getUser(player).getFaction();
        Home home = faction.getHome();

        if (home == null) {
            new Message("No faction home set").fail().send(player, false);
            return 0;
        }

        if (player.getServer() == null) return 0;

        Optional<RegistryKey<World>> worldKey = player.getServer().getWorldRegistryKeys().stream().filter(key -> Objects.equals(key.getValue(), new Identifier(home.level))).findAny();

        if (worldKey.isEmpty()) {
            new Message("Cannot find dimension").fail().send(player, false);
            return 0;
        }

        ServerWorld world = player.getServer().getWorld(worldKey.get());

        if (checkLimitToClaim(faction, world, new BlockPos(home.x, home.y, home.z))) {
            new Message("Cannot warp home to an unclaimed chunk").fail().send(player, false);
            return 0;
        }

        DamageRecord damageRecord = player.getDamageTracker().getMostRecentDamage();
        if (damageRecord == null || player.age - damageRecord.getEntityAge() > FactionsMod.CONFIG.HOME.DAMAGE_COOLDOWN) {
            player.teleport(
                    world,
                    home.x, home.y, home.z,
                    home.yaw, home.pitch
            );
            new Message("Warped to faction home").send(player, false);
        } else {
            new Message("Cannot warp while in combat").fail().send(player, false);
        }
        return 1;
    }

    private int set(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        if (checkLimitToClaim(faction, player.getWorld(), player.getBlockPos())) {
            new Message("Cannot set home to an unclaimed chunk").fail().send(player, false);
            return 0;
        }

        Home home = new Home(
            faction.getID(),
            player.getX(), player.getY(), player.getZ(),
            player.getHeadYaw(), player.getPitch(),
            player.getWorld().getRegistryKey().getValue().toString()
        );

        faction.setHome(home);
        new Message(
            "Home set to %.2f, %.2f, %.2f by %s",
            home.x,
            home.y,
            home.z,
            player.getName().getString()
        ).send(faction);
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
        return CommandManager
            .literal("home")
            .requires(Requires.multiple(Requires.isMember(), s -> FactionsMod.CONFIG.HOME != null, Requires.hasPerms("factions.home", 0)))
            .executes(this::go)
            .then(
                CommandManager.literal("set")
                .requires(Requires.multiple(Requires.hasPerms("factions.home.set", 0), Requires.isLeader()))
                .executes(this::set)
            )
            .build();
    }
}
