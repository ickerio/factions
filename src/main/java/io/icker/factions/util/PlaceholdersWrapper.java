package io.icker.factions.util;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.User;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import static eu.pb4.placeholders.api.PlaceholderResult.invalid;
import static eu.pb4.placeholders.api.PlaceholderResult.value;
import static eu.pb4.placeholders.api.Placeholders.register;
import static io.icker.factions.FactionsMod.MODID;
import static java.lang.Integer.*;
import static net.minecraft.util.Formatting.DARK_GRAY;

public class PlaceholdersWrapper {
    public static final Identifier FACTION_NAME_ID = new Identifier(MODID, "name");
    public static final Identifier FACTION_DESCRIPTION_ID = new Identifier(MODID, "description");
    public static final Identifier FACTION_STATE_ID = new Identifier(MODID, "state");
    public static final Identifier FACTION_POWER_ID = new Identifier(MODID, "power");
    public static final Identifier FACTION_POWER_FORMATTED_ID = new Identifier(MODID, "power_formatted");
    public static final Identifier FACTION_MAX_POWER_ID = new Identifier(MODID, "max_power");
    public static final Identifier FACTION_PLAYER_POWER_ID = new Identifier(MODID, "player_power");
    public static final Identifier FACTION_PLAYER_MAX_POWER_ID = new Identifier(MODID, "player_max_power");
    public static final Identifier FACTION_REQUIRED_POWER_ID = new Identifier(MODID, "required_power");
    public static final Identifier FACTION_REQUIRED_POWER_FORMATTED_ID = new Identifier(MODID, "required_power_formatted");
    public static final String NULL_STRING = "N/A";
    public static final Text NULL_TEXT = Text.literal(NULL_STRING).formatted(DARK_GRAY);

    public static void init() {
        register(FACTION_NAME_ID, (ctx, argument) -> {
            if (!ctx.hasPlayer()) return invalid("No Player!");
            assert ctx.player() != null;

            Text r = NULL_TEXT;

            final var member = User.get(ctx.player().getUuid());
            if (member == null) return value(r);


            final var faction = member.getFaction();

            if (faction != null)
                r = Text.literal(faction.getName()).formatted(member.getFaction().getColor());

            return value(r);
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
//              TODO(samu): import per-player power patch
//              FIXME(samu): Using normal max power formula instead of per-player max power
                final int red = mapBoundRange(faction.calculateMaxPower(), 0, 170, 255, faction.getPower());
                final int green = mapBoundRange(0, faction.calculateMaxPower(), 170, 255, faction.getPower());
                r = Text.literal("" + faction.getPower()).setStyle(Style.EMPTY.withColor(TextColor.parse("#" + toHexString(red) + toHexString(green) + "AA")));
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

//          TODO(samu): import per-player power patch
//          FIXME(samu): Using normal max power formula instead of per-player max power
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
                r = "" + faction.getClaims().size() * FactionsMod.CONFIG.CLAIM_WEIGHT;

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
                final int reqPower = faction.getClaims().size() * FactionsMod.CONFIG.CLAIM_WEIGHT;
                final int red = mapBoundRange(0, faction.getPower(), 85, 255, reqPower);
                r = Text.literal("" + reqPower).setStyle(Style.EMPTY.withColor(TextColor.parse("#" + toHexString(red) + "5555")));
            }

            return value(r);
        });
    }

    @SuppressWarnings("all")
    private static int mapBoundRange(int a1, int a2, int b1, int b2, int s) {
        return min(b2, max(b1, b1 + ((s - a1) * (b2 - b1)) / (a2 - a1)));
    }
}
