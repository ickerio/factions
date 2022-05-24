package io.icker.factions.util;

import java.util.function.Predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.api.persistents.Member.Rank;
import io.icker.factions.config.Config;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public interface Command {
    public LiteralCommandNode<ServerCommandSource> getNode();

    public interface Requires {
        boolean run(Member member);

        public static Predicate<ServerCommandSource> isFactionless() {
            return require(member -> !member.isInFaction());
        }

        public static Predicate<ServerCommandSource> isMember() {
            return require(member -> member.isInFaction());
        }

        public static Predicate<ServerCommandSource> isCommander() {
            return require(member -> member.getRank() == Rank.COMMANDER || member.getRank() == Rank.LEADER || member.getRank() == Rank.OWNER);
        }

        public static Predicate<ServerCommandSource> isLeader() {
            return require(member -> member.getRank() == Rank.LEADER || member.getRank() == Rank.OWNER);
        }

        public static Predicate<ServerCommandSource> isOwner() {
            return require(member -> member.getRank() == Rank.OWNER);
        }
        
        public static Predicate<ServerCommandSource> isAdmin() {
            return source -> source.hasPermissionLevel(Config.REQUIRED_BYPASS_LEVEL);
        }

        public static Predicate<ServerCommandSource> hasPerms(String permission, int defaultValue) {
            return source -> PermissionsInnerWrapper.check(source, permission, defaultValue);
        }

        public static Predicate<ServerCommandSource> require(Requires req) {
            return source -> {
                try {
                    ServerPlayerEntity entity = source.getPlayer();
                    Member member = Member.get(entity.getUuid());
                    return req.run(member);
                } catch (CommandSyntaxException e) {
                    return false;
                }
            };
        }
    }

    public interface Suggests {
        String[] run(Member member);

        public static SuggestionProvider<ServerCommandSource> allFactions() {
            return suggest(member -> 
                Faction.all()
                    .stream()
                    .map(f -> f.getName())
                    .toArray(String[]::new)
            );
        }

        public static SuggestionProvider<ServerCommandSource> openFactions() {
            return suggest(member -> 
                Faction.all()
                    .stream()
                    .filter(f -> f.isOpen())
                    .map(f -> f.getName())
                    .toArray(String[]::new)
            );
        }

        public static SuggestionProvider<ServerCommandSource> suggest(Suggests sug) {
            return (context, builder) -> {
                try {
                    ServerPlayerEntity entity = context.getSource().getPlayer();
                    Member member = Member.get(entity.getUuid());
                    for (String suggestion : sug.run(member)) {
                        builder.suggest(suggestion);
                    }
                } catch (CommandSyntaxException e) {}
                return builder.buildFuture();
            };
        }
    }
}