package io.icker.factions.util;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import xyz.nucleoid.server.translations.api.Localization;
import xyz.nucleoid.server.translations.api.language.ServerLanguage;

import java.util.function.Function;

public class PlaceholdersWrapper {
    private static final Text UNFORMATTED_NULL = Text.translatable("factions.papi.factionless");
    private static final Text FORMATTED_NULL =
            UNFORMATTED_NULL.copy().formatted(Formatting.DARK_GRAY);

    private static void register(String identifier, Function<User, Text> handler) {
        Placeholders.register(
                Identifier.of(FactionsMod.MODID, identifier),
                (ctx, argument) -> {
                    if (!ctx.hasPlayer())
                        return PlaceholderResult.invalid(
                                Localization.raw(
                                        "argument.entity.notfound.player",
                                        ServerLanguage.getLanguage(FactionsMod.CONFIG.LANGUAGE)));

                    User member = User.get(ctx.player().getUuid());
                    return PlaceholderResult.value(handler.apply(member));
                });
    }

    public static void init() {
        register(
                "name",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return FORMATTED_NULL;

                    return Text.literal(faction.getName())
                            .formatted(member.getFaction().getColor());
                });

        register(
                "colorless_name",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return FORMATTED_NULL;

                    return Text.of(faction.getName());
                });

        register(
                "chat",
                (member) -> {
                    if (member.chat == User.ChatMode.GLOBAL || !member.isInFaction())
                        return Text.translatable("factions.papi.chat.global");

                    return Text.translatable("factions.papi.chat.faction");
                });

        register(
                "rank",
                (member) -> {
                    if (!member.isInFaction()) return FORMATTED_NULL;

                    return Text.of(member.getRankName());
                });

        register(
                "color",
                (member) -> {
                    if (!member.isInFaction()) return Text.of("reset");

                    return Text.of(member.getFaction().getColor().getName());
                });

        register(
                "description",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return FORMATTED_NULL;

                    return Text.of(faction.getDescription());
                });

        register(
                "state",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return UNFORMATTED_NULL;

                    return Text.of(String.valueOf(faction.isOpen()));
                });

        register(
                "power",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return UNFORMATTED_NULL;

                    return Text.of(String.valueOf(faction.getPower()));
                });

        register(
                "power_formatted",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return FORMATTED_NULL;

                    int red =
                            mapBoundRange(
                                    faction.calculateMaxPower(), 0, 170, 255, faction.getPower());
                    int green =
                            mapBoundRange(
                                    0, faction.calculateMaxPower(), 170, 255, faction.getPower());
                    return Text.literal(String.valueOf(faction.getPower()))
                            .setStyle(Style.EMPTY.withColor(rgbToInt(red, green, 170)));
                });

        register(
                "max_power",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return UNFORMATTED_NULL;

                    return Text.of(String.valueOf(faction.calculateMaxPower()));
                });

        register(
                "player_power",
                (member) -> {
                    return Text.of(String.valueOf(FactionsMod.CONFIG.POWER.MEMBER));
                });

        register(
                "required_power",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return UNFORMATTED_NULL;

                    return Text.of(
                            String.valueOf(
                                    faction.getClaims().size()
                                            * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT));
                });

        register(
                "required_power_formatted",
                (member) -> {
                    Faction faction = member.getFaction();
                    if (faction == null) return FORMATTED_NULL;

                    int reqPower =
                            faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
                    int red = mapBoundRange(0, faction.getPower(), 85, 255, reqPower);
                    return Text.literal(String.valueOf(reqPower))
                            .setStyle(Style.EMPTY.withColor(rgbToInt(red, 85, 85)));
                });
    }

    private static int rgbToInt(int red, int green, int blue) {
        return (red & 255 << 16) | (green & 255 << 8) | (blue & 255);
    }

    private static int mapBoundRange(
            int from_min, int from_max, int to_min, int to_max, int value) {
        return Math.min(
                to_max,
                Math.max(
                        to_min,
                        to_min + ((value - from_min) * (to_max - to_min)) / (from_max - from_min)));
    }
}
