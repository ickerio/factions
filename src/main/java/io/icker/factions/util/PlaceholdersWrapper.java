package io.icker.factions.util;

import io.icker.factions.api.persistents.User;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import static eu.pb4.placeholders.api.PlaceholderResult.invalid;
import static eu.pb4.placeholders.api.PlaceholderResult.value;
import static eu.pb4.placeholders.api.Placeholders.parsePlaceholder;
import static eu.pb4.placeholders.api.Placeholders.register;
import static io.icker.factions.FactionsMod.MODID;
import static java.lang.Integer.*;
import static net.minecraft.text.Text.empty;
import static net.minecraft.text.Text.literal;
import static net.minecraft.util.Formatting.DARK_GRAY;
import static net.minecraft.util.Formatting.GRAY;

public class PlaceholdersWrapper {
    public static final Identifier FACTION_NAME_ID = new Identifier(MODID, "name");
    public static final Identifier FACTION_CHAT_ID = new Identifier(MODID, "chat");
    public static final Identifier FACTION_RANK_ID = new Identifier(MODID, "rank");
    public static final Identifier FACTION_COLOR_ID = new Identifier(MODID, "color");
    public static final Identifier FACTION_DESCRIPTION_ID = new Identifier(MODID, "description");
    public static final Identifier FACTION_STATE_ID = new Identifier(MODID, "state");
    public static final Identifier FACTION_POWER_ID = new Identifier(MODID, "power");
    public static final Identifier FACTION_POWER_FORMATTED_ID = new Identifier(MODID, "power_formatted");
    public static final Identifier FACTION_MAX_POWER_ID = new Identifier(MODID, "max_power");
    public static final Identifier FACTION_PLAYER_POWER_ID = new Identifier(MODID, "player_power");
    public static final Identifier FACTION_PLAYER_POWER_FORMATTED_ID = new Identifier(MODID, "player_power_formatted");
    public static final Identifier FACTION_PLAYER_MAX_POWER_ID = new Identifier(MODID, "player_max_power");
    public static final Identifier FACTION_REQUIRED_POWER_ID = new Identifier(MODID, "required_power");
    public static final Identifier FACTION_REQUIRED_POWER_FORMATTED_ID = new Identifier(MODID, "required_power_formatted");
    public static final Identifier FACTION_POWER_STATS_ID = new Identifier(MODID, "power_stats");
    public static final Identifier FACTION_POWER_STATS_FORMATTED_ID = new Identifier(MODID, "power_stats_formatted");
    public static final String NULL_STRING = "N/A";
    public static final Text NULL_TEXT = literal(NULL_STRING).formatted(DARK_GRAY);

    public static void init() {
        register(FACTION_NAME_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            Text r = NULL_TEXT;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);


            final var faction = member.getFaction();

            if (faction != null)
                r = literal(faction.getName()).formatted(member.getFaction().getColor());

            return value(r);
        });

        register(FACTION_CHAT_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            Text r = Text.of("Faction Chat");

            final var member = User.get(ctx.player().getUuid());
            if (member.chat == User.ChatMode.GLOBAL || !member.isInFaction())
                r = Text.of("Global Chat");

            return value(r);
        });

        register(FACTION_RANK_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            Text r = NULL_TEXT;

            final var member = User.get(ctx.player().getUuid());

            if (member.isInFaction())
                r = Text.of(member.getRankName());

            return value(r);
        });

        register(FACTION_COLOR_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            final var member = User.get(ctx.player().getUuid());

            if (member.isInFaction()) {
                return value(member.getFaction().getColor().getName());
            }

            return value(NULL_TEXT);
        });

        register(FACTION_DESCRIPTION_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            String r = NULL_STRING;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);

            final var faction = member.getFaction();

            if (faction != null)
                r = faction.getDescription();

            return value(r);
        });

        register(FACTION_STATE_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            String r = NULL_STRING;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);

            final var faction = member.getFaction();

            if (faction != null)
                r = "" + faction.isOpen();

            return value(r);
        });

        register(FACTION_POWER_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            String r = NULL_STRING;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);

            final var faction = member.getFaction();

            if (faction != null)
                r = "" + faction.getPower();

            return value(r);
        });

        register(FACTION_POWER_FORMATTED_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            Text r = NULL_TEXT;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);

            final var faction = member.getFaction();

            if (faction != null) {
                final int red = mapBoundRange(faction.calculateMaxPower(), 0, 170, 255, faction.getPower());
                final int green = mapBoundRange(0, faction.calculateMaxPower(), 170, 255, faction.getPower());
                r = literal("" + faction.getPower()).setStyle(Style.EMPTY.withColor(TextColor.parse("#" + toHexString(red) + toHexString(green) + "AA")));
            }

            return value(r);
        });

        register(FACTION_MAX_POWER_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            String r = NULL_STRING;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);

            final var faction = member.getFaction();

            if (faction != null) r = "" + faction.calculateMaxPower();

            return value(r);
        });

        register(FACTION_PLAYER_POWER_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value("" + User.getPower(ctx.player().getUuid()));

            String r = "" + member.getPower();

            return value(r);
        });

        register(FACTION_PLAYER_POWER_FORMATTED_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            final var member = User.get(ctx.player().getUuid());

            final int red = mapBoundRange(member.getMaxPower(), 0, 170, 255, member.getPower());
            final int green = mapBoundRange(0, member.getMaxPower(), 170, 255, member.getPower());

            return value(literal("" + member.getPower()).setStyle(Style.EMPTY.withColor(TextColor.parse("#" + toHexString(red) + toHexString(green) + "AA"))));
        });

        register(FACTION_PLAYER_MAX_POWER_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value("" + User.getMaxPower(ctx.player().getUuid()));

            String r = "" + member.getMaxPower();

            return value(r);
        });

        register(FACTION_REQUIRED_POWER_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            String r = NULL_STRING;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);

            final var faction = member.getFaction();

            if (faction != null)
                r = "" + faction.calculateRequiredPower();

            return value(r);
        });

        register(FACTION_REQUIRED_POWER_FORMATTED_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            Text r = NULL_TEXT;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);

            final var faction = member.getFaction();


            if (faction != null) {
                final int reqPower = faction.calculateRequiredPower();
                final int red = mapBoundRange(0, faction.getPower(), 85, 255, reqPower);
                r = literal("" + reqPower).setStyle(Style.EMPTY.withColor(TextColor.parse("#" + toHexString(red) + "5555")));
            }

            return value(r);
        });

        register(FACTION_POWER_STATS_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            final var member = User.get(ctx.player().getUuid());
            if (member.isInFaction()) {
                return value(
                        empty()
                        .append(parsePlaceholder(FACTION_POWER_ID, argument, ctx).text())
                        .append(literal("/").formatted(DARK_GRAY))
                        .append(parsePlaceholder(FACTION_REQUIRED_POWER_ID, argument, ctx).text())
                        .append(literal("/").formatted(DARK_GRAY))
                        .append(parsePlaceholder(FACTION_MAX_POWER_ID, argument, ctx).text())
                );
            } else {
                return value(
                        empty()
                        .append(parsePlaceholder(FACTION_PLAYER_POWER_ID, argument, ctx).text())
                        .append(literal("/").formatted(DARK_GRAY))
                        .append(parsePlaceholder(FACTION_PLAYER_MAX_POWER_ID, argument, ctx).text())
                );
            }
        });

        register(FACTION_POWER_STATS_FORMATTED_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            final var member = User.get(ctx.player().getUuid());
            if (member.isInFaction()) {
                return value(
                        empty()
                        .append(parsePlaceholder(FACTION_POWER_FORMATTED_ID, argument, ctx).text())
                        .append(literal("/").formatted(DARK_GRAY))
                        .append(parsePlaceholder(FACTION_REQUIRED_POWER_FORMATTED_ID, argument, ctx).text())
                        .append(literal("/").formatted(DARK_GRAY))
                        .append(parsePlaceholder(FACTION_MAX_POWER_ID, argument, ctx).text().copy().formatted(GRAY))
                );
            } else {
                return value(
                        empty()
                        .append(parsePlaceholder(FACTION_PLAYER_POWER_FORMATTED_ID, argument, ctx).text())
                        .append(literal("/").formatted(DARK_GRAY))
                        .append(parsePlaceholder(FACTION_PLAYER_MAX_POWER_ID, argument, ctx).text().copy().formatted(GRAY))
                );
            }
        });
    }

    @SuppressWarnings("all")    //math utils
    private static int mapBoundRange(int a1, int a2, int b1, int b2, int s) {
        try {
            return min(b2, max(b1, b1 + ((s - a1) * (b2 - b1)) / (a2 - a1)));
        } catch (ArithmeticException ignored){
            return 0;
        }
    }
}
