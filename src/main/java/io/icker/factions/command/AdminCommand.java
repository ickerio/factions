package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.ui.AdminGui;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public class AdminCommand implements Command {
    private int gui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        // Show UI
        new AdminGui(player);

        return 1;
    }

    private int bypass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        User user = User.get(player.getUUID());
        boolean bypass = !user.bypass;
        user.bypass = bypass;

        new Message(Component.translatable("factions.gui.admin.options.bypass.success"))
                .filler("·")
                .add(
                        new Message(
                                        user.bypass
                                                ? Component.translatable("options.on")
                                                : Component.translatable("options.off"))
                                .format(user.bypass ? ChatFormatting.GREEN : ChatFormatting.RED))
                .send(player, false);

        return 1;
    }

    private int reload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        FactionsMod.dynmap.reloadAll();
        new Message(Component.translatable("factions.gui.admin.options.reload_dynmap.success"))
                .send(context.getSource().getPlayerOrException(), false);
        return 1;
    }

    private int power(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));
        int power = IntegerArgumentType.getInteger(context, "power");

        target.addAdminPower(power);

        if (power != 0) {
            if (power > 0) {
                new Message(
                                Component.translatable(
                                        "factions.gui.power.success.added.faction",
                                        player.getName().getString(),
                                        power))
                        .send(target);
                new Message(Component.translatable("factions.gui.power.success.added.admin", power))
                        .send(player, false);
            } else {
                new Message(
                                Component.translatable(
                                        "factions.gui.power.success.removed.faction",
                                        player.getName().getString(),
                                        power))
                        .send(target);
                new Message(
                                Component.translatable(
                                        "factions.gui.power.success.removed.admin", power))
                        .send(player, false);
            }
        } else {
            new Message(Component.translatable("factions.gui.power.fail.nochange"))
                    .fail()
                    .send(player, false);
        }

        return 1;
    }

    private int spoof(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        User user = User.get(player.getUUID());

        String name = StringArgumentType.getString(context, "player");

        User target;

        Optional<GameProfile> profile;
        if ((profile = source.getServer().services().profileResolver().fetchByName(name))
                .isPresent()) {
            target = User.get(profile.get().id());
        } else {
            try {
                target = User.get(UUID.fromString(name));
            } catch (Exception e) {
                new Message(Component.translatable("factions.gui.spoof.fail.no_player", name))
                        .format(ChatFormatting.RED)
                        .send(player, false);
                return 0;
            }
        }

        user.setSpoof(target);

        new Message(Component.translatable("factions.gui.spoof.success", name)).send(player, false);

        return 1;
    }

    private int clearSpoof(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        User user = User.get(player.getUUID());

        user.setSpoof(null);

        new Message(Component.translatable("factions.gui.admin.options.spoof.clear.success"))
                .send(player, false);

        return 1;
    }

    private int audit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        for (int i = 0; i < 4; i++) {
            Claim.audit();
            Faction.audit();
            User.audit();
        }

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        if (player != null) {
            new Message(Component.translatable("factions.gui.admin.options.audit.success"))
                    .send(player, false);
        }

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("admin")
                .requires(
                        Requires.hasPerms(
                                "factions.admin.gui", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .executes(this::gui)
                .then(
                        Commands.literal("bypass")
                                .requires(
                                        Requires.hasPerms(
                                                "factions.admin.bypass",
                                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                                .executes(this::bypass))
                .then(
                        Commands.literal("reload")
                                .requires(
                                        Requires.multiple(
                                                Requires.hasPerms(
                                                        "factions.admin.reload",
                                                        FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL),
                                                source -> FactionsMod.dynmap != null))
                                .executes(this::reload))
                .then(
                        Commands.literal("power")
                                .requires(
                                        Requires.hasPerms(
                                                "factions.admin.power",
                                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                                .then(
                                        Commands.argument("power", IntegerArgumentType.integer())
                                                .then(
                                                        Commands.argument(
                                                                        "faction",
                                                                        StringArgumentType
                                                                                .greedyString())
                                                                .suggests(Suggests.allFactions())
                                                                .executes(this::power))))
                .then(
                        Commands.literal("spoof")
                                .requires(
                                        Requires.hasPerms(
                                                "factions.admin.spoof",
                                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                                .then(
                                        Commands.argument("player", StringArgumentType.string())
                                                .suggests(Suggests.allPlayers())
                                                .executes(this::spoof))
                                .executes(this::clearSpoof))
                .then(
                        Commands.literal("audit")
                                .requires(
                                        Requires.hasPerms(
                                                "factions.admin.audit",
                                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                                .executes(this::audit))
                .build();
    }
}
