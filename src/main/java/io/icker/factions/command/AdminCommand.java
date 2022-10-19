package io.icker.factions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class AdminCommand implements Command {
    private int bypass(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        User user = User.get(player.getUuid());
        boolean bypass = !user.bypass;
        user.bypass = bypass;

        new Message("Successfully toggled claim bypass")
                .filler("·")
                .add(
                    new Message(user.bypass ? "ON" : "OFF")
                        .format(user.bypass ? Formatting.GREEN : Formatting.RED)
                )
                .send(player, false);

        return 1;
    }

    private int reload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FactionsMod.dynmap.reloadAll();
        new Message("Reloaded dynmap marker").send(context.getSource().getPlayer(), false);
        return 1;
    }

    private int power(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));
        int power = IntegerArgumentType.getInteger(context, "power");

        int adjusted = target.adjustPower(power);
        if (adjusted != 0) {
            if (power > 0) {
                new Message(
                    "Admin %s added %d power",
                    player.getName().getString(),
                    adjusted
                ).send(target);
                new Message(
                    "Added %d power",
                    adjusted
                ).send(player, false);
            } else {
                new Message(
                    "Admin %s removed %d power",
                    player.getName().getString(),
                    adjusted
                ).send(target);
                new Message(
                    "Removed %d power",
                    adjusted
                ).send(player, false);
            }
        } else {
            new Message("Could not change power").fail().send(player, false);
        }

        return 1;
    }

    private int lives(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        ServerPlayerEntity targetEntity = EntityArgumentType.getPlayer(context, "player");
        User target = User.get(targetEntity.getUuid());

        int numLives = IntegerArgumentType.getInteger(context, "numLives");

        target.lives = numLives;

        new Message("Set %s's lives to %d", targetEntity.getName().getString(), numLives).send(player, false);
        new Message("Your lives have been set to %d by %s", numLives, player.getName().getString()).send(targetEntity, false);

        return 1;
    }

    private int spoof(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getUuid());

        ServerPlayerEntity targetEntity = EntityArgumentType.getPlayer(context, "player");
        User target = User.get(targetEntity.getUuid());

        user.setSpoof(target);

        new Message("Set spoof to player %s", targetEntity.getName().getString()).send(player, false);

        return 1;
    }

    private int clearSpoof(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getUuid());

        user.setSpoof(null);

        new Message("Cleared spoof").send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("admin")
            .then(
                CommandManager.literal("bypass")
                .requires(Requires.hasPerms("factions.admin.bypass", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .executes(this::bypass)
            )
            .then(
                CommandManager.literal("reload")
                .requires(Requires.multiple(Requires.hasPerms("factions.admin.reload", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL), source -> FactionsMod.dynmap != null))
                .executes(this::reload)
            )
            .then(
                CommandManager.literal("power")
                .requires(Requires.hasPerms("factions.admin.power", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("power", IntegerArgumentType.integer())
                    .then(
                        CommandManager.argument("faction", StringArgumentType.greedyString())
                        .suggests(Suggests.allFactions())
                        .executes(this::power)
                    )
                )
            )
            .then(
                CommandManager.literal("spoof")
                .requires(Requires.hasPerms("factions.admin.spoof", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::spoof)
                )
                .executes(this::clearSpoof)
            )
            .then(
                CommandManager.literal("lives")
                .requires(Requires.hasPerms("factions.admin.lives", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("numLives", IntegerArgumentType.integer())
                        .then(
                            CommandManager.argument("player", EntityArgumentType.player())
                            .executes(this::lives)
                        )
                )
            )
            .build();
    }
}
