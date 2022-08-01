package io.icker.factions.util;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.War;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Predicate;


public interface Command {
    public LiteralCommandNode<ServerCommandSource> getNode();
    public static final boolean permissions = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    public interface Requires {
        boolean run(User user);

        @SafeVarargs
        public static Predicate<ServerCommandSource> multiple(Predicate<ServerCommandSource>... args) {
            return source -> {
                for (Predicate<ServerCommandSource> predicate : args) {
                    if (!predicate.test(source)) return false;
                }

                return true;
            };
        }

        public static Predicate<ServerCommandSource> isFactionless() {
            return require(user -> !user.isInFaction());
        }

        public static Predicate<ServerCommandSource> isMember() {
            return require(user -> user.isInFaction());
        }

        public static Predicate<ServerCommandSource> isCommander() {
            return require(user -> user.rank == User.Rank.COMMANDER || user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER);
        }

        public static Predicate<ServerCommandSource> isLeader() {
            return require(user -> user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER);
        }

        public static Predicate<ServerCommandSource> isOwner() {
            return require(user -> user.rank == User.Rank.OWNER);
        }
        
        public static Predicate<ServerCommandSource> isAdmin() {
            return source -> source.hasPermissionLevel(FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL);
        }

        public static Predicate<ServerCommandSource> hasPerms(String permission, int defaultValue) {
            return source -> !permissions || Permissions.check(source, permission, defaultValue);
        }

        public static Predicate<ServerCommandSource> require(Requires req) {
            return source -> {
                ServerPlayerEntity entity = source.getPlayer();
                User user = Command.getUser(entity);
                return req.run(user);
            };
        }
    }

    public interface Suggests {
        String[] run(User user);

        public static SuggestionProvider<ServerCommandSource> allFactions() {
            return allFactions(true);
        }

        public static SuggestionProvider<ServerCommandSource> allFactions(boolean includeYou) {
            return suggest(user -> 
                Faction.all()
                    .stream()
                    .filter(f -> includeYou || !user.isInFaction() || !user.getFaction().getID().equals(f.getID()))
                    .map(f -> f.getName())
                    .toArray(String[]::new)
            );
        }

        public static SuggestionProvider<ServerCommandSource> atWar() {
            return suggest(user ->
                    user.getFaction().getWars()
                            .stream()
                            .map(War::getName)
                            .toArray(String[]::new)
            );
        }

        public static SuggestionProvider<ServerCommandSource> joinableWars() {
            return suggest(user -> War.all()
                .stream()
                .filter(war ->
                    user.getFaction().getMutualAllies()
                        .stream()
                        .map(rel -> Faction.get(rel.target))
                        .anyMatch(faction -> war.getSourceTeam().contains(faction) || war.getTargetTeam().contains(faction))
                ).map(War::getName)
                .toArray(String[]::new)

            );
        }

        public static SuggestionProvider<ServerCommandSource> eligibleForWar() {
            return suggest(user -> {
                    Faction source = user.getFaction();
                    return source.getEnemiesWith()
                        .stream()
                        .filter(rel -> rel.aggression >= FactionsMod.CONFIG.WAR.AGGRESSION_LEVEL)
                        .map(rel -> Faction.get(rel.target).getName())
                        .toArray(String[]::new);
                }
            );
        }

        public static SuggestionProvider<ServerCommandSource> openFactions() {
            return suggest(user ->
                Faction.all()
                    .stream()
                    .filter(f -> f.isOpen())
                    .map(f -> f.getName())
                    .toArray(String[]::new)
            );
        }

        public static SuggestionProvider<ServerCommandSource> openInvitedFactions() {
            return suggest(user ->
                Faction.all()
                    .stream()
                    .filter(f -> f.isOpen() || f.isInvited(user.getID()))
                    .map(f -> f.getName())
                    .toArray(String[]::new)
            );
        }

        public static SuggestionProvider<ServerCommandSource> suggest(Suggests sug) {
            return (context, builder) -> {
                ServerPlayerEntity entity = context.getSource().getPlayer();
                User user = User.get(entity.getUuid());
                for (String suggestion : sug.run(user)) {
                    builder.suggest(suggestion);
                }
                return builder.buildFuture();
            };
        }
    }

    public static User getUser(ServerPlayerEntity player) {
        User user = User.get(player.getUuid());
        if (user.getSpoof() == null) {
            return user;
        }
        return user.getSpoof();
    }
}