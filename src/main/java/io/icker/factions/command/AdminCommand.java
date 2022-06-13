package io.icker.factions.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.PlayerManager;
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

    private int disband(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));

        new Message("An admin disbanded the faction").send(target);
        target.remove();

        new Message("Faction has been removed").send(player, false);

        PlayerManager manager = source.getServer().getPlayerManager();
        for (ServerPlayerEntity p : manager.getPlayerList()) {
            manager.sendCommandTree(p);
        }
        return 1;
    }

    private int kick(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        ServerPlayerEntity targetEntity = EntityArgumentType.getPlayer(context, "player");
        User target = User.get(targetEntity.getUuid());

        if (!target.isInFaction()) {
            new Message("%s is not in a faction", targetEntity.getName().getString()).fail().send(player, false);
            return 0;
        }

        Faction faction = target.getFaction();

        if (target.rank == User.Rank.OWNER) {
            new Message("%s is the owner of %s and can't be removed, instead run ", targetEntity.getName().getString(), faction.getName())
                .add(
                    new Message("/f admin disband")
                    .hover("Click to run")
                    .click(String.format("/f admin disband %s", faction.getName()))
                )
                .add(" or ")
                .add(
                    new Message("/f admin transfer")
                    .hover("Click to run")
                    .click(String.format("/f admin transfer %s", targetEntity.getName().getString()))
                ).fail().send(player, false);

            return 0;
        }

        new Message("An admin kicked you from %s", faction.getName()).send(targetEntity, false);
        target.leaveFaction();

        new Message("%s has been kicked from %s", targetEntity.getName().getString(), faction.getName()).send(player, false);

        PlayerManager manager = source.getServer().getPlayerManager();
        for (ServerPlayerEntity p : manager.getPlayerList()) {
            manager.sendCommandTree(p);
        }
        return 1;
    }

    private int transfer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        ServerPlayerEntity targetEntity = EntityArgumentType.getPlayer(context, "player");
        User target = User.get(targetEntity.getUuid());

        if (!target.isInFaction()) {
            new Message("%s is not in a faction", targetEntity.getName().getString()).fail().send(player, false);
            return 0;
        }

        Faction faction = target.getFaction();

        if (target.rank == User.Rank.OWNER) {
            new Message("%s is already the owner of %s", targetEntity.getName().getString(), faction.getName()).fail().send(player, false);
            return 0;
        }

        User owner = faction.getUsers().stream().filter((user) -> user.rank == User.Rank.OWNER).findFirst().orElse(target);
        ServerPlayerEntity ownerEntity = source.getServer().getPlayerManager().getPlayer(owner.getID());

        owner.rank = User.Rank.MEMBER;
        target.rank = User.Rank.OWNER;

        new Message("An admin has made you owner of %s", faction.getName()).send(targetEntity, false);
        new Message("An admin has stripped your ownership of %s and given it to %s", faction.getName(), targetEntity.getName().getString()).send(targetEntity, false);

        new Message("%s has been given owner ship of %s from %s", targetEntity.getName().getString(), faction.getName(), ownerEntity.getName().getString()).send(player, false);

        PlayerManager manager = source.getServer().getPlayerManager();
        for (ServerPlayerEntity p : manager.getPlayerList()) {
            manager.sendCommandTree(p);
        }
        return 1;
    }

    private int power(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));
        int power = IntegerArgumentType.getInteger(context, "power");

        int adjusted = target.adjustPower(power);
        if (adjusted != 0) {
            if (power > 0) {
                new Message("Admin %s added %d power", player.getName().getString(), adjusted).send(target);
                new Message("Added %d power", adjusted).send(player, false);
            } else {
                new Message("Admin %s removed %d power", player.getName().getString(), adjusted).send(target);
                new Message("Removed %d power", adjusted).send(player, false);
            }
        } else {
            new Message("Could not change power").fail().send(player, false);
        }

        return 1;
    }

    private int safe(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));

        PlayerEvents.OPEN_SAFE.invoker().onOpenSafe(player, target);

        return 1;
    }

    private int spoof(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getUuid());

        ServerPlayerEntity targetEntity = EntityArgumentType.getPlayer(context, "player");
        User target = User.get(targetEntity.getUuid());

        user.setSpoof(target);

        return 1;
    }

    private int clearSpoof(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getUuid());

        user.setSpoof(null);

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
                CommandManager.literal("disband")
                .requires(Requires.hasPerms("factions.admin.disband", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions())
                    .executes(this::disband)
                )
            )
            .then(
                CommandManager.literal("safe")
                .requires(Requires.hasPerms("factions.admin.safe", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions())
                    .executes(this::safe)
                )
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
                CommandManager.literal("kick")
                .requires(Requires.hasPerms("factions.admin.kick", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::kick)
                )
            )
            .then(
                CommandManager.literal("transfer")
                .requires(Requires.hasPerms("factions.admin.transfer", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::transfer)
                )
            )
            .then(
                CommandManager.literal("spoof")
                .requires(Requires.hasPerms("factions.admin.spoof", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::spoof)
                )
            )
            .then(
                CommandManager.literal("clearSpoof")
                .requires(Requires.hasPerms("factions.admin.spoof.clear", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .executes(this::clearSpoof)
            )
            .build();
    }
}
