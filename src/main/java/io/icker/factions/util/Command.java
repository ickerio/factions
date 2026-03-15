package io.icker.factions.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;

import me.lucko.fabric.api.permissions.v0.Permissions;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.ProfileResolver;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public interface Command {
    public LiteralCommandNode<CommandSourceStack> getNode();

    public static final boolean permissions =
            FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    public interface Requires {
        boolean run(User user);

        @SafeVarargs
        public static Predicate<CommandSourceStack> multiple(
                Predicate<CommandSourceStack>... args) {
            return source -> {
                for (Predicate<CommandSourceStack> predicate : args) {
                    if (!predicate.test(source)) return false;
                }

                return true;
            };
        }

        public static Predicate<CommandSourceStack> isFactionless() {
            return require(user -> !user.isInFaction());
        }

        public static Predicate<CommandSourceStack> isMember() {
            return require(user -> user.isInFaction());
        }

        public static Predicate<CommandSourceStack> isCommander() {
            return require(
                    user ->
                            user.rank == User.Rank.COMMANDER
                                    || user.rank == User.Rank.LEADER
                                    || user.rank == User.Rank.OWNER);
        }

        public static Predicate<CommandSourceStack> isLeader() {
            return require(user -> user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER);
        }

        public static Predicate<CommandSourceStack> isOwner() {
            return require(user -> user.rank == User.Rank.OWNER);
        }

        public static Predicate<CommandSourceStack> isAdmin() {
            return source ->
                    source.permissions()
                            .hasPermission(
                                    new Permission.HasCommandLevel(
                                            PermissionLevel.byId(
                                                    FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL)));
        }

        public static Predicate<CommandSourceStack> hasPerms(String permission, int defaultValue) {
            return source -> {
                if (permissions) {
                    return Permissions.check(source, permission, defaultValue);
                } else {
                    return source.permissions()
                            .hasPermission(
                                    new Permission.HasCommandLevel(PermissionLevel.byId(defaultValue)));
                }
            };
        }

        public static Predicate<CommandSourceStack> require(Requires req) {
            return source -> {
                ServerPlayer entity = source.getPlayer();
                if (entity == null) {
                    return false;
                }
                User user = Command.getUser(entity);
                return req.run(user);
            };
        }
    }

    public interface Suggests {
        String[] run(User user);

        public static SuggestionProvider<CommandSourceStack> allFactions() {
            return allFactions(true);
        }

        public static SuggestionProvider<CommandSourceStack> allFactions(boolean includeYou) {
            return suggest(
                    user ->
                            Faction.all().stream()
                                    .filter(
                                            f ->
                                                    includeYou
                                                            || !user.isInFaction()
                                                            || !user.getFaction()
                                                                    .getID()
                                                                    .equals(f.getID()))
                                    .map(f -> f.getName())
                                    .toArray(String[]::new));
        }

        static SuggestionProvider<CommandSourceStack> allPlayers() {
            return (context, builder) -> {
                ProfileResolver resolver =
                        context.getSource().getServer().services().profileResolver();

                for (User user : User.all()) {
                    Optional<GameProfile> player;
                    if ((player = resolver.fetchById(user.getID())).isPresent()) {
                        builder.suggest(player.get().name());
                    } else {
                        builder.suggest(user.getID().toString());
                    }
                }
                return builder.buildFuture();
            };
        }

        static SuggestionProvider<CommandSourceStack> allPlayersInYourFactionButYou() {
            return (context, builder) -> {
                ProfileResolver resolver =
                        context.getSource().getServer().services().profileResolver();
                ServerPlayer entity = context.getSource().getPlayerOrException();
                User currentUser = User.get(entity.getUUID());

                if (!currentUser.isInFaction()) {
                    return builder.buildFuture();
                }

                for (User user : User.all()) {
                    if (user.getID().equals(currentUser.getID())
                            || !user.isInFaction()
                            || !user.getFaction().equals(currentUser.getFaction())) {
                        continue;
                    }
                    Optional<GameProfile> player;
                    if ((player = resolver.fetchById(user.getID())).isPresent()) {
                        builder.suggest(player.get().name());
                    } else {
                        builder.suggest(user.getID().toString());
                    }
                }
                return builder.buildFuture();
            };
        }

        public static SuggestionProvider<CommandSourceStack> openFactions() {
            return suggest(
                    user ->
                            Faction.all().stream()
                                    .filter(f -> f.isOpen())
                                    .map(f -> f.getName())
                                    .toArray(String[]::new));
        }

        public static SuggestionProvider<CommandSourceStack> openInvitedFactions() {
            return suggest(
                    user ->
                            Faction.all().stream()
                                    .filter(f -> f.isOpen() || f.isInvited(user.getID()))
                                    .map(f -> f.getName())
                                    .toArray(String[]::new));
        }

        public static <T extends Enum<T>> SuggestionProvider<CommandSourceStack> enumSuggestion(
                Class<T> clazz) {
            return suggest(
                    user ->
                            Arrays.stream(clazz.getEnumConstants())
                                    .map(Enum::toString)
                                    .toArray(String[]::new));
        }

        public static SuggestionProvider<CommandSourceStack> suggest(Suggests sug) {
            return (context, builder) -> {
                ServerPlayer entity = context.getSource().getPlayerOrException();
                User user = User.get(entity.getUUID());
                for (String suggestion : sug.run(user)) {
                    builder.suggest(suggestion);
                }
                return builder.buildFuture();
            };
        }
    }

    public static User getUser(ServerPlayer player) {
        User user = User.get(player.getUUID());
        if (user.getSpoof() == null) {
            return user;
        }
        return user.getSpoof();
    }
}
