package io.icker.factions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import static io.icker.factions.api.persistents.User.get;
import static net.minecraft.text.Text.of;
import static net.minecraft.util.Formatting.GREEN;
import static net.minecraft.util.Formatting.RED;

public class AdminCommand implements Command {
    private int bypass(@NotNull CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        User user = get(player.getUuid());
        user.bypass = !user.bypass;

        new Message("Successfully toggled claim bypass")
                .filler("·")
                .add(
                    new Message(user.bypass ? "ON" : "OFF")
                        .format(user.bypass ? GREEN : RED)
                )
                .send(player, false);

        return 1;
    }

    private int reload(@NotNull CommandContext<ServerCommandSource> context) {
        FactionsMod.dynmap.reloadAll();
        context.getSource().sendFeedback(of("Reloaded dynmap marker"), true);   //NOTE(CamperSamu): Avoid locking a reload command to be player-only, use the vanilla feedback function.
        return 1;
    }

    private int power(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        final User target = get(EntityArgumentType.getPlayer(context, "player").getUuid());
        final int power = IntegerArgumentType.getInteger(context, "power");

        int adjusted = target.addPower(power);
        if (adjusted != 0) {
            if (power > 0) {
                new Message(
                    "Admin %s added %d power",
                    player.getName().getString(),
                    adjusted
                ).send(player, false);
                new Message(
                    "Added %d power",
                    adjusted
                ).send(player, false);
            } else {
                new Message(
                    "Admin %s removed %d power",
                    player.getName().getString(),
                    adjusted
                ).send(player, false);
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

    private int spoof(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        User user = get(player.getUuid());

        ServerPlayerEntity targetEntity = EntityArgumentType.getPlayer(context, "player");
        User target = get(targetEntity.getUuid());

        user.setSpoof(target);

        new Message("Set spoof to player %s", targetEntity.getName().getString()).send(player, false);

        return 1;
    }

    private int clearSpoof(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        User user = get(player.getUuid());

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
                        CommandManager.argument("player", EntityArgumentType.player())
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
            .build();
    }
}
