package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.FillerText;
import io.icker.factions.text.Message;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;
import java.util.UUID;

public class AdminCommand implements Command {
    private int bypass(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        User user = User.get(player.getUuid());
        boolean bypass = !user.bypass;
        user.bypass = bypass;

        new Message()
                .append(new TranslatableText("Successfully toggled claim bypass"))
                .append(new FillerText("·"))
                .append(
                    new TranslatableText(user.bypass ? "translate:on" : "translate:off")
                )
                .send(player, false);

        return 1;
    }

    private int reload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FactionsMod.dynmap.reloadAll();
        new Message().append(new TranslatableText("translate:admin.reload")).send(context.getSource().getPlayer(), false);
        return 1;
    }

    private int power(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));
        int power = IntegerArgumentType.getInteger(context, "power");

        int adjusted = target.adjustPower(power);
        if (adjusted != 0) {
            if (power > 0) {
                new Message().append(
                    new TranslatableText(
                        "translate:admin.power.add",
                        player.getName().getString(),
                        adjusted
                    )
                ).send(target);

                new Message().append(
                        new TranslatableText(
                                "translate:admin.power.add.self",
                                adjusted
                        )
                ).send(player, false);
            } else {
                new Message().append(
                        new TranslatableText(
                                "translate:admin.power.remove",
                                player.getName().getString(),
                                adjusted
                        )
                ).send(target);

                new Message().append(
                        new TranslatableText(
                                "translate:admin.power.add.remove",
                                adjusted
                        )
                ).send(player, false);
            }
        } else {
            new Message().append(new TranslatableText("translate:admin.power.error").fail()).send(player, false);
        }

        return 1;
    }

    private int spoof(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getUuid());

        String name = StringArgumentType.getString(context, "player");

        User target;

        Optional<GameProfile> profile;
        if ((profile = source.getServer().getUserCache().findByName(name)).isPresent()) {
            target = User.get(profile.get().getId());
        } else {
            target = User.get(UUID.fromString(name));
        }

        user.setSpoof(target);

        new Message().append(new TranslatableText("translate:admin.spoof", name)).send(player, false);

        return 1;
    }

    private int clearSpoof(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getUuid());

        user.setSpoof(null);

        new Message().append(new TranslatableText("translate:admin.spoof.clear")).send(player, false);

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
                    CommandManager.argument("player", StringArgumentType.string())
                        .suggests(Suggests.allPlayers())
                        .executes(this::spoof)
                )
                .executes(this::clearSpoof)
            )
            .build();
    }
}
