package io.icker.factions.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TradeStageConfig} defaults and Gson round-trip.
 *
 * <p>The stage coordinates and level are calibrated to the trade stage build supplied by
 * the project owner. This suite pins:
 * <ul>
 *   <li>the exact defaults — a regression in any of these silently teleports every
 *       trade participant into the wrong world or off the platform;</li>
 *   <li>the {@link com.google.gson.annotations.SerializedName} bindings — Gson names in
 *       the on-disk {@code factions.json} MUST match {@code camelCase} keys or existing
 *       user configs stop deserialising;</li>
 *   <li>the {@code enabled} default — 3.3 ships {@code /f trade} enabled by default.</li>
 * </ul>
 */
class TradeStageConfigLoadTest {

    private static Gson gson() {
        return new GsonBuilder().create();
    }

    @Test
    void defaults_requesterCoordinatesMatchSuppliedStage() {
        TradeStageConfig cfg = new TradeStageConfig();

        assertEquals(1.050, cfg.REQUESTER_X, 0.001,
                "requester X must match the stage-supplied spawn point");
        assertEquals(160.125, cfg.REQUESTER_Y, 0.001,
                "requester Y must match the stage-supplied spawn point");
        assertEquals(-27.500, cfg.REQUESTER_Z, 0.001,
                "requester Z must match the stage-supplied spawn point");
        assertEquals(89.7f, cfg.REQUESTER_YAW, 0.01f,
                "requester yaw must face the recipient across the stage");
        assertEquals(3.2f, cfg.REQUESTER_PITCH, 0.01f,
                "requester pitch must match the stage-supplied camera angle");
    }

    @Test
    void defaults_recipientCoordinatesMatchSuppliedStage() {
        TradeStageConfig cfg = new TradeStageConfig();

        assertEquals(-2.050, cfg.RECIPIENT_X, 0.001,
                "recipient X must match the stage-supplied spawn point");
        assertEquals(160.125, cfg.RECIPIENT_Y, 0.001,
                "recipient Y must match the stage-supplied spawn point");
        assertEquals(-27.482, cfg.RECIPIENT_Z, 0.001,
                "recipient Z must match the stage-supplied spawn point");
        assertEquals(-90.9f, cfg.RECIPIENT_YAW, 0.01f,
                "recipient yaw must face the requester across the stage");
        assertEquals(3.2f, cfg.RECIPIENT_PITCH, 0.01f,
                "recipient pitch must match the stage-supplied camera angle");
    }

    @Test
    void defaults_levelIsOverworld() {
        TradeStageConfig cfg = new TradeStageConfig();

        assertEquals("minecraft:overworld", cfg.LEVEL,
                "the stage lives in the overworld — changing this silently breaks every "
                        + "trade because the stage build only exists in that dimension");
    }

    @Test
    void defaults_enabledIsTrue() {
        TradeStageConfig cfg = new TradeStageConfig();

        assertTrue(cfg.ENABLED,
                "3.3 ships /f trade enabled by default; only flip to false in a hotfix");
    }

    @Test
    void gsonRoundTrip_preservesAllFields() {
        TradeStageConfig original = new TradeStageConfig();
        original.ENABLED = false;
        original.REQUESTER_X = 12.5;
        original.REQUESTER_Y = 200.0;
        original.REQUESTER_Z = -100.25;
        original.REQUESTER_YAW = 45.0f;
        original.REQUESTER_PITCH = -10.0f;
        original.RECIPIENT_X = -12.5;
        original.RECIPIENT_Y = 200.0;
        original.RECIPIENT_Z = -100.75;
        original.RECIPIENT_YAW = -135.0f;
        original.RECIPIENT_PITCH = -10.0f;
        original.LEVEL = "minecraft:the_nether";

        String json = gson().toJson(original);
        TradeStageConfig loaded = gson().fromJson(json, TradeStageConfig.class);

        assertNotNull(loaded, "Gson must decode a non-null instance");
        assertEquals(original.ENABLED, loaded.ENABLED);
        assertEquals(original.REQUESTER_X, loaded.REQUESTER_X, 0.0001);
        assertEquals(original.REQUESTER_Y, loaded.REQUESTER_Y, 0.0001);
        assertEquals(original.REQUESTER_Z, loaded.REQUESTER_Z, 0.0001);
        assertEquals(original.REQUESTER_YAW, loaded.REQUESTER_YAW, 0.0001f);
        assertEquals(original.REQUESTER_PITCH, loaded.REQUESTER_PITCH, 0.0001f);
        assertEquals(original.RECIPIENT_X, loaded.RECIPIENT_X, 0.0001);
        assertEquals(original.RECIPIENT_Y, loaded.RECIPIENT_Y, 0.0001);
        assertEquals(original.RECIPIENT_Z, loaded.RECIPIENT_Z, 0.0001);
        assertEquals(original.RECIPIENT_YAW, loaded.RECIPIENT_YAW, 0.0001f);
        assertEquals(original.RECIPIENT_PITCH, loaded.RECIPIENT_PITCH, 0.0001f);
        assertEquals(original.LEVEL, loaded.LEVEL);
    }

    @Test
    void gsonRoundTrip_defaultsSurviveEncoding() {
        TradeStageConfig original = new TradeStageConfig();

        String json = gson().toJson(original);
        TradeStageConfig loaded = gson().fromJson(json, TradeStageConfig.class);

        assertEquals(original.ENABLED, loaded.ENABLED);
        assertEquals(original.REQUESTER_X, loaded.REQUESTER_X, 0.0001);
        assertEquals(original.REQUESTER_Y, loaded.REQUESTER_Y, 0.0001);
        assertEquals(original.REQUESTER_Z, loaded.REQUESTER_Z, 0.0001);
        assertEquals(original.REQUESTER_YAW, loaded.REQUESTER_YAW, 0.0001f);
        assertEquals(original.REQUESTER_PITCH, loaded.REQUESTER_PITCH, 0.0001f);
        assertEquals(original.RECIPIENT_X, loaded.RECIPIENT_X, 0.0001);
        assertEquals(original.RECIPIENT_Y, loaded.RECIPIENT_Y, 0.0001);
        assertEquals(original.RECIPIENT_Z, loaded.RECIPIENT_Z, 0.0001);
        assertEquals(original.RECIPIENT_YAW, loaded.RECIPIENT_YAW, 0.0001f);
        assertEquals(original.RECIPIENT_PITCH, loaded.RECIPIENT_PITCH, 0.0001f);
        assertEquals(original.LEVEL, loaded.LEVEL);
    }

    @Test
    void gsonEmitsExpectedCamelCaseKeys() {
        TradeStageConfig cfg = new TradeStageConfig();

        String json = gson().toJson(cfg);

        assertTrue(json.contains("\"enabled\""),
                "SerializedName(\"enabled\") must survive — user configs use this key");
        assertTrue(json.contains("\"requesterX\""),
                "SerializedName(\"requesterX\") must survive — user configs use this key");
        assertTrue(json.contains("\"requesterY\""),
                "SerializedName(\"requesterY\") must survive — user configs use this key");
        assertTrue(json.contains("\"requesterZ\""),
                "SerializedName(\"requesterZ\") must survive — user configs use this key");
        assertTrue(json.contains("\"requesterYaw\""),
                "SerializedName(\"requesterYaw\") must survive — user configs use this key");
        assertTrue(json.contains("\"requesterPitch\""),
                "SerializedName(\"requesterPitch\") must survive — user configs use this key");
        assertTrue(json.contains("\"recipientX\""),
                "SerializedName(\"recipientX\") must survive — user configs use this key");
        assertTrue(json.contains("\"recipientY\""),
                "SerializedName(\"recipientY\") must survive — user configs use this key");
        assertTrue(json.contains("\"recipientZ\""),
                "SerializedName(\"recipientZ\") must survive — user configs use this key");
        assertTrue(json.contains("\"recipientYaw\""),
                "SerializedName(\"recipientYaw\") must survive — user configs use this key");
        assertTrue(json.contains("\"recipientPitch\""),
                "SerializedName(\"recipientPitch\") must survive — user configs use this key");
        assertTrue(json.contains("\"level\""),
                "SerializedName(\"level\") must survive — user configs use this key");
    }

    @Test
    void gsonDeserialisesFromExplicitCamelCaseJson() {
        String json =
                "{"
                        + "\"enabled\":true,"
                        + "\"requesterX\":1.050,"
                        + "\"requesterY\":160.125,"
                        + "\"requesterZ\":-27.500,"
                        + "\"requesterYaw\":89.7,"
                        + "\"requesterPitch\":3.2,"
                        + "\"recipientX\":-2.050,"
                        + "\"recipientY\":160.125,"
                        + "\"recipientZ\":-27.482,"
                        + "\"recipientYaw\":-90.9,"
                        + "\"recipientPitch\":3.2,"
                        + "\"level\":\"minecraft:overworld\""
                        + "}";

        TradeStageConfig cfg = gson().fromJson(json, TradeStageConfig.class);

        assertNotNull(cfg);
        assertTrue(cfg.ENABLED);
        assertEquals(1.050, cfg.REQUESTER_X, 0.001);
        assertEquals(160.125, cfg.REQUESTER_Y, 0.001);
        assertEquals(-27.500, cfg.REQUESTER_Z, 0.001);
        assertEquals(89.7f, cfg.REQUESTER_YAW, 0.01f);
        assertEquals(3.2f, cfg.REQUESTER_PITCH, 0.01f);
        assertEquals(-2.050, cfg.RECIPIENT_X, 0.001);
        assertEquals(160.125, cfg.RECIPIENT_Y, 0.001);
        assertEquals(-27.482, cfg.RECIPIENT_Z, 0.001);
        assertEquals(-90.9f, cfg.RECIPIENT_YAW, 0.01f);
        assertEquals(3.2f, cfg.RECIPIENT_PITCH, 0.01f);
        assertEquals("minecraft:overworld", cfg.LEVEL);
    }
}
