package io.icker.factions.core;

import io.icker.factions.api.persistents.Faction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class AnnouncerManager {
    private static final int FADE_BASE_RGB = 0x222222;
    private static final int WHITE_FALLBACK_RGB = 0xFFFFFF;

    private AnnouncerManager() {}

    public static Component buildAnnouncement(Faction faction) {
        if (faction == null) {
            return Component.translatableWithFallback(
                            "factions.announcer.wilderness", "Wilderness")
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE);
        }

        return Component.literal(faction.getName())
                .withStyle(ChatFormatting.BOLD, faction.getColor());
    }

    /** Returns a dimmed copy of the announcement for fade-in steps. step: 0 (darkest) .. steps-1 (full). */
    public static Component buildFadeStep(Faction faction, int step, int steps) {
        int targetRgb = resolveTargetRgb(faction);
        int interpolated = interpolateRgb(FADE_BASE_RGB, targetRgb, step, steps);
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(interpolated)).withBold(true);

        if (faction == null) {
            return Component.translatableWithFallback(
                            "factions.announcer.wilderness", "Wilderness")
                    .setStyle(style);
        }

        return Component.literal(faction.getName()).setStyle(style);
    }

    private static int resolveTargetRgb(Faction faction) {
        ChatFormatting formatting = faction == null ? ChatFormatting.WHITE : faction.getColor();
        TextColor color = TextColor.fromLegacyFormat(formatting);
        return color != null ? color.getValue() : WHITE_FALLBACK_RGB;
    }

    private static int interpolateRgb(int baseRgb, int targetRgb, int step, int steps) {
        int clampedStep = Math.max(0, Math.min(step, steps - 1));
        float fraction = (clampedStep + 1) / (float) steps;

        int baseR = (baseRgb >> 16) & 0xFF;
        int baseG = (baseRgb >> 8) & 0xFF;
        int baseB = baseRgb & 0xFF;

        int targetR = (targetRgb >> 16) & 0xFF;
        int targetG = (targetRgb >> 8) & 0xFF;
        int targetB = targetRgb & 0xFF;

        int r = Math.round(baseR + (targetR - baseR) * fraction);
        int g = Math.round(baseG + (targetG - baseG) * fraction);
        int b = Math.round(baseB + (targetB - baseB) * fraction);

        return (r << 16) | (g << 8) | b;
    }
}
