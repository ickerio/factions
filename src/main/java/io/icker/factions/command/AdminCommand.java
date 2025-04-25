package io.icker.factions.command;

import java.util.Optional;
import java.util.UUID;
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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AdminCommand implements Command {
    private int gui(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

        // Show UI
        new AdminGui(player);

        return 1;
    }

    private int bypass(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        User user = User.get(player.getUuid());
        boolean bypass = !user.bypass;
        user.bypass = bypass;

        new Message(Text.translatable("factions.gui.admin.options.bypass.success")).filler("·")
                .add(new Message(
                        user.bypass ? Text.translatable("options.on") : Text.translatable("options.off"))
                        .format(user.bypass ? Formatting.GREEN : Formatting.RED))
                .send(player, false);

        return 1;
    }

    private int reload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        FactionsMod.dynmap.reloadAll();
        new Message(Text.translatable("factions.gui.admin.options.reload_dynmap.success")).send(
                context.getSource().getPlayer(),
                false);
        return 1;
    }

    private int power(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));
        int power = IntegerArgumentType.getInteger(context, "power");

        target.addAdminPower(power);

        if (power != 0) {
            if (power > 0) {
                new Message(
                        Text.translatable("factions.gui.power.success.added.faction",
                                player.getName().getString(),
                                power))
                        .send(target);
                new Message(Text.translatable("factions.gui.power.success.added.admin", power))
                        .send(player, false);
            } else {
                new Message(
                        Text.translatable("factions.gui.power.success.removed.faction",
                                player.getName().getString(),
                                power))
                        .send(target);
                new Message(Text.translatable("factions.gui.power.success.removed.admin", power))
                        .send(player, false);
            }
        } else {
            new Message(Text.translatable("factions.gui.power.error.nochange")).fail().send(player, false);
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
            try {
                target = User.get(UUID.fromString(name));
            } catch (Exception e) {
                new Message(Text.translatable("factions.gui.spoof.error.no_player", name)).format(Formatting.RED)
                        .send(player, false);
                return 0;
            }
        }

        user.setSpoof(target);

        new Message(Text.translatable("factions.gui.spoof.success", name)).send(player, false);

        return 1;
    }

    private int clearSpoof(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getUuid());

        user.setSpoof(null);

        new Message(Text.translatable("factions.gui.admin.options.spoof.clear.success")).send(player, false);

        return 1;
    }

    private int audit(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        for (int i = 0; i < 4; i++) {
            Claim.audit();
            Faction.audit();
            User.audit();
        }

        if (player != null) {
            new Message(Text.translatable("factions.gui.admin.options.audit.success")).send(player, false);
        }

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("admin")
                .requires(Requires.hasPerms("factions.admin.gui",
                        FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .executes(this::gui)
                .then(CommandManager.literal("bypass")
                        .requires(Requires.hasPerms("factions.admin.bypass",
                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                        .executes(this::bypass))
                .then(CommandManager.literal("reload")
                        .requires(Requires.multiple(
                                Requires.hasPerms("factions.admin.reload",
                                        FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL),
                                source -> FactionsMod.dynmap != null))
                        .executes(this::reload))
                .then(CommandManager.literal("power")
                        .requires(
                                Requires.hasPerms("factions.admin.power",
                                        FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                        .then(CommandManager.argument("power", IntegerArgumentType.integer())
                                .then(CommandManager
                                        .argument("faction", StringArgumentType.greedyString())
                                        .suggests(Suggests.allFactions()).executes(this::power))))
                .then(CommandManager.literal("spoof")
                        .requires(Requires.hasPerms("factions.admin.spoof",
                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                        .then(CommandManager.argument("player", StringArgumentType.string())
                                .suggests(Suggests.allPlayers()).executes(this::spoof))
                        .executes(this::clearSpoof))
                .then(CommandManager.literal("audit")
                        .requires(Requires.hasPerms("factions.admin.audit",
                                FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                        .executes(this::audit))
                .build();
    }
}
