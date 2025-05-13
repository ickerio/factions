package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.UUID;

public class KickCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        String name = StringArgumentType.getString(context, "player");

        User target;

        Optional<GameProfile> profile;
        if ((profile = source.getServer().getUserCache().findByName(name)).isPresent()) {
            target = User.get(profile.get().getId());
        } else {
            try {
                target = User.get(UUID.fromString(name));
            } catch (Exception e) {
                new Message(Text.translatable("factions.gui.spoof.fail.no_player", name))
                        .format(Formatting.RED)
                        .send(player, false);
                return 0;
            }
        }

        if (target.getID().equals(player.getUuid())) {
            new Message(Text.translatable("factions.command.kick.fail.self"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        User selfUser = Command.getUser(player);

        if (target.getFaction() == null || !target.getFaction().equals(selfUser.getFaction())) {
            new Message(Text.translatable("factions.command.kick.fail.other_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (selfUser.rank == User.Rank.LEADER
                && (target.rank == User.Rank.LEADER || target.rank == User.Rank.OWNER)) {
            new Message(Text.translatable("factions.command.kick.fail.high_rank"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        ServerPlayerEntity targetPlayer =
                player.getServer().getPlayerManager().getPlayer(target.getID());

        target.leaveFaction();

        if (targetPlayer != null) {
            context.getSource().getServer().getPlayerManager().sendCommandTree(targetPlayer);

            new Message(
                            Text.translatable(
                                    "factions.command.kick.success.subject",
                                    player.getName().getString()))
                    .send(targetPlayer, false);
        }

        new Message(
                        Text.translatable(
                                "factions.command.kick.success.actor",
                                profile.map((found_profile) -> found_profile.getName())
                                        .orElse("unknown")))
                .send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("kick")
                .requires(
                        Requires.multiple(
                                Requires.isLeader(), Requires.hasPerms("factions.kick", 0)))
                .then(
                        CommandManager.argument("player", StringArgumentType.string())
                                .suggests(Suggests.allPlayersInYourFactionButYou())
                                .executes(this::run))
                .build();
    }
}
