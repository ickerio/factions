package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import xyz.nucleoid.server.translations.api.Localization;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KickCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

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

        if (target.getID().equals(player.getUUID())) {
            new Message(Component.translatable("factions.command.kick.fail.self"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        User selfUser = Command.getUser(player);

        if (target.getFaction() == null || !target.getFaction().equals(selfUser.getFaction())) {
            new Message(Component.translatable("factions.command.kick.fail.other_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        if (selfUser.rank == User.Rank.LEADER
                && (target.rank == User.Rank.LEADER || target.rank == User.Rank.OWNER)) {
            new Message(Component.translatable("factions.command.kick.fail.high_rank"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        ServerPlayer targetPlayer =
                player.level().getServer().getPlayerList().getPlayer(target.getID());

        target.leaveFaction();

        if (targetPlayer != null) {
            context.getSource().getServer().getPlayerList().sendPlayerPermissionLevel(targetPlayer);

            new Message(
                            Component.translatable(
                                    "factions.command.kick.success.subject",
                                    player.getName().getString()))
                    .send(targetPlayer, false);
        }

        new Message(
                        Component.translatable(
                                "factions.command.kick.success.actor",
                                profile.map((found_profile) -> found_profile.name())
                                        .orElse(
                                                Localization.raw(
                                                        "factions.gui.members.entry.unknown_player",
                                                        player))))
                .send(player, false);

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("kick")
                .requires(
                        Requires.multiple(
                                Requires.isLeader(), Requires.hasPerms("factions.kick", 0)))
                .then(
                        Commands.argument("player", StringArgumentType.string())
                                .suggests(Suggests.allPlayersInYourFactionButYou())
                                .executes(this::run))
                .build();
    }
}
