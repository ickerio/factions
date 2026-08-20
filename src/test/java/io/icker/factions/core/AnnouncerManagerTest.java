package io.icker.factions.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import org.junit.jupiter.api.Test;

class AnnouncerManagerTest {
    private static final int FADE_STEPS = 6;

    @Test
    void wildernessAnnouncementIsNotNull() {
        Component component = AnnouncerManager.buildAnnouncement(null);

        assertNotNull(component);
    }

    @Test
    void wildernessAnnouncementContainsWildernessText() {
        Component component = AnnouncerManager.buildAnnouncement(null);

        assertTrue(component.getString().contains("Wilderness"));
    }

    @Test
    void wildernessAnnouncementHasNoDescriptionSeparator() {
        Component component = AnnouncerManager.buildAnnouncement(null);

        assertTrue(!component.getString().contains("•"));
    }

    @Test
    void wildernessAnnouncementIsBold() {
        Component component = AnnouncerManager.buildAnnouncement(null);

        assertTrue(
                component.getStyle().isBold(),
                "wilderness must be bold so it matches faction announcements");
    }

    @Test
    void wildernessFadeStepFinalMatchesTargetWhite() {
        Component finalStep = AnnouncerManager.buildFadeStep(null, FADE_STEPS - 1, FADE_STEPS);

        TextColor color = finalStep.getStyle().getColor();
        assertNotNull(color);
        TextColor expected = TextColor.fromLegacyFormat(ChatFormatting.WHITE);
        assertNotNull(expected);
        assertEquals(expected.getValue(), color.getValue());
    }

    @Test
    void wildernessFadeStepFirstIsDarkerThanFinal() {
        Component firstStep = AnnouncerManager.buildFadeStep(null, 0, FADE_STEPS);
        Component finalStep = AnnouncerManager.buildFadeStep(null, FADE_STEPS - 1, FADE_STEPS);

        TextColor firstColor = firstStep.getStyle().getColor();
        TextColor finalColor = finalStep.getStyle().getColor();
        assertNotNull(firstColor);
        assertNotNull(finalColor);
        assertNotEquals(firstColor.getValue(), finalColor.getValue());
        assertTrue(
                brightness(firstColor.getValue()) < brightness(finalColor.getValue()),
                "first fade step must be dimmer than final fade step");
    }

    private static int brightness(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r + g + b;
    }
}
