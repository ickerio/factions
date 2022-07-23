package io.icker.factions.util;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public interface Command {
    boolean permissions = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    static User getUser(@NotNull ServerPlayerEntity player) {
        User user = User.get(player.getUuid());
        if (user.getSpoof() == null) {
            return user;
        }
        return user.getSpoof();
    }

    LiteralCommandNode<ServerCommandSource> getNode();

    interface Requires {
        @Contract(pure = true)
        @SafeVarargs
        static @NotNull Predicate<ServerCommandSource> multiple(Predicate<ServerCommandSource>... args) {
            return source -> {
                for (Predicate<ServerCommandSource> predicate : args) {
                    if (!predicate.test(source)) return false;
                }

                return true;
            };
        }

        @Contract(pure = true)
        static @NotNull Predicate<ServerCommandSource> isFactionless() {
            return require(user -> !user.isInFaction());
        }

        @Contract(pure = true)
        static @NotNull Predicate<ServerCommandSource> isMember() {
            return require(User::isInFaction);
        }

        @Contract(pure = true)
        static @NotNull Predicate<ServerCommandSource> isCommander() {
            return require(user -> user.rank == User.Rank.COMMANDER || user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER);
        }

        @Contract(pure = true)
        static @NotNull Predicate<ServerCommandSource> isLeader() {
            return require(user -> user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER);
        }

        @Contract(pure = true)
        static @NotNull Predicate<ServerCommandSource> isOwner() {
            return require(user -> user.rank == User.Rank.OWNER);
        }

        @Contract(pure = true)
        @SuppressWarnings("unused") //util
        static @NotNull Predicate<ServerCommandSource> isAdmin() {
            return source -> source.hasPermissionLevel(FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL);
        }

        @Contract(pure = true)
        static @NotNull Predicate<ServerCommandSource> hasPerms(String permission, int defaultValue) {
            return source -> !permissions || Permissions.check(source, permission, defaultValue);
        }

        @Contract(pure = true)
        static @NotNull Predicate<ServerCommandSource> require(Requires req) {
            return source -> {
                ServerPlayerEntity player = source.getPlayer();
                if (player == null)
                    return false;  // Confirm that it's a player executing the command and not an entity with /execute (or console)
                User user = Command.getUser(player);
                return req.run(user);
            };
        }

        boolean run(User user);
    }

    interface Suggests {
        @Contract(pure = true)
        static @NotNull SuggestionProvider<ServerCommandSource> allFactions() {
            return allFactions(true);
        }

        @Contract(pure = true)
        static @NotNull SuggestionProvider<ServerCommandSource> allFactions(boolean includeYou) {
            return suggest(user ->
                    Faction.all()
                            .stream()
                            .filter(f -> includeYou || !user.isInFaction() || !user.getFaction().getID().equals(f.getID()))
                            .map(Faction::getName)
                            .toArray(String[]::new)
            );
        }

        @Contract(pure = true)
        @SuppressWarnings("unused") //util
        static @NotNull SuggestionProvider<ServerCommandSource> openFactions() {
            return suggest(user ->
                    Faction.all()
                            .stream()
                            .filter(Faction::isOpen)
                            .map(Faction::getName)
                            .toArray(String[]::new)
            );
        }

        @Contract(pure = true)
        static @NotNull SuggestionProvider<ServerCommandSource> openInvitedFactions() {
            return suggest(user ->
                    Faction.all()
                            .stream()
                            .filter(f -> f.isOpen() || f.isInvited(user.getID()))
                            .map(Faction::getName)
                            .toArray(String[]::new)
            );
        }

        @Contract(pure = true)
        static @NotNull SuggestionProvider<ServerCommandSource> suggest(Suggests sug) {
            return (context, builder) -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                if (player == null)
                    return null;  // Confirm that it's a player executing the command and not an entity with /execute
                User user = User.get(player.getUuid());
                for (String suggestion : sug.run(user)) {
                    builder.suggest(suggestion);
                }
                return builder.buildFuture();
            };
        }

        String[] run(User user);
    }
}