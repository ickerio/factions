package io.icker.factions.client.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.client.ClaimCache;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.MinimapProcessor;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.render.module.ModuleRenderContext;

@Pseudo
@Environment(EnvType.CLIENT)
@Mixin(targets = "xaero.hud.minimap.module.MinimapRenderer", remap = false)
public class MixinMinimapRenderer {
    private static boolean errorLogged;
    private static final java.util.ArrayList<ClaimCache.PackedRect> visibleBuffer = new java.util.ArrayList<>(256);

    private static volatile int shapeInit;
    private static Object shapeConfigOption;
    private static Object northLockedConfigOption;
    private static Method configGetEffective;
    private static Field hudModInstance;
    private static Method hudModGetHudConfigs;
    private static Method configChannelGetClientConfigManager;

    private static Object readConfig(Object option) {
        try {
            if (shapeInit == 0) {
                Class<?> opts = Class.forName("xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions");
                shapeConfigOption = opts.getField("SHAPE").get(null);
                northLockedConfigOption = opts.getField("NORTH_LOCKED").get(null);
                Class<?> hudMod = Class.forName("xaero.common.HudMod");
                hudModInstance = hudMod.getField("INSTANCE");
                hudModGetHudConfigs = hudMod.getMethod("getHudConfigs");
                Class<?> configChannel = Class.forName("xaero.lib.common.config.channel.ConfigChannel");
                configChannelGetClientConfigManager = configChannel.getMethod("getClientConfigManager");
                Class<?> ccm = Class.forName("xaero.lib.client.config.ClientConfigManager");
                Class<?> optionCls = Class.forName("xaero.lib.common.config.option.ConfigOption");
                configGetEffective = ccm.getMethod("getEffective", optionCls);
                shapeInit = 1;
            }
            if (shapeInit != 1 || option == null) return null;
            Object inst = hudModInstance.get(null);
            if (inst == null) return null;
            Object chan = hudModGetHudConfigs.invoke(inst);
            Object mgr = configChannelGetClientConfigManager.invoke(chan);
            return configGetEffective.invoke(mgr, option);
        } catch (Throwable t) {
            shapeInit = 2;
            return null;
        }
    }

    private static boolean isRoundMinimap() {
        Object val = readConfig(shapeConfigOption);
        if (val instanceof Integer i) return i != 0;
        if (val instanceof Boolean b) return b;
        return false;
    }

    private static boolean isNorthLocked() {
        Object val = readConfig(northLockedConfigOption);
        if (val instanceof Boolean b) return b;
        if (val instanceof Integer i) return i != 0;
        return false;
    }

    private static void drawClaims(java.util.List<ClaimCache.PackedRect> visible, GuiGraphicsExtractor graphics) {
        for (ClaimCache.PackedRect claim : visible) {
            graphics.fill(claim.minX(), claim.minZ(), claim.maxX() + 1, claim.maxZ() + 1, claim.argb());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            MinimapSession session,
            ModuleRenderContext context,
            GuiGraphicsExtractor graphics,
            float tickDelta,
            CallbackInfo ci) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) return;

            String dimensionId = minecraft.level.dimension().identifier().toString();
            var claims = ClaimCache.getForDim(dimensionId);
            if (claims.isEmpty()) return;

            MinimapProcessor processor = session.getProcessor();
            if (processor.isCaveModeDisplayed()) return;
            double blocksPerPixel = processor.getMinimapZoom();
            int minimapSize = processor.getMinimapSize();
            if (!(blocksPerPixel > 0.0) || minimapSize <= 0) return;

            double displayScale = (double) context.w / minimapSize;
            float pixelsPerBlock = (float) (displayScale / blocksPerPixel);
            float centerX = context.x + context.w * 0.5f;
            float centerY = context.y + context.h * 0.5f;
            float playerX = (float) minecraft.player.getX(tickDelta);
            float playerZ = (float) minecraft.player.getZ(tickDelta);
            float yawDeg = minecraft.player.getViewYRot(tickDelta);
            float rot = isNorthLocked() ? 0f : (float) Math.toRadians(-(yawDeg + 180f));
            double blocksAcross = minimapSize * blocksPerPixel;
            double viewRadius = blocksAcross * 0.75 + 32.0;

            int pad = 3;
            Matrix3x2fStack pose = graphics.pose();
            boolean round = isRoundMinimap();

            java.util.ArrayList<ClaimCache.PackedRect> visible = visibleBuffer;
            visible.clear();
            for (ClaimCache.PackedRect claim : claims) {
                if (claim.maxX() + 1 < playerX - viewRadius) continue;
                if (claim.minX() > playerX + viewRadius) continue;
                if (claim.maxZ() + 1 < playerZ - viewRadius) continue;
                if (claim.minZ() > playerZ + viewRadius) continue;
                visible.add(claim);
            }
            if (visible.isEmpty()) return;

            if (round) {
                double radius = Math.min(context.w, context.h) * 0.5 - pad;
                if (!(radius > 0)) return;
                double radiusSq = radius * radius;
                int rowTop = (int) Math.floor(centerY - radius);
                int rowBottom = (int) Math.ceil(centerY + radius);
                int stripH = 2; // strip height: 1=per-row (slow, smooth), higher=faster but slightly jagged

                for (int y = rowTop; y < rowBottom; y += stripH) {
                    int y1 = Math.min(y + stripH, rowBottom);
                    double topDy = y - centerY;
                    double botDy = (y1 - 1) - centerY;
                    double dyWorst = Math.max(Math.abs(topDy), Math.abs(botDy));
                    double sq = radiusSq - dyWorst * dyWorst;
                    if (sq <= 0.0) continue;
                    double dx = Math.sqrt(sq);
                    int rx0 = (int) Math.ceil(centerX - dx);
                    int rx1 = (int) Math.floor(centerX + dx);
                    if (rx1 <= rx0) continue;
                    graphics.enableScissor(rx0, y, rx1, y1);
                    try {
                        pose.pushMatrix();
                        try {
                            pose.translate(centerX, centerY);
                            pose.rotate(rot);
                            pose.scale(pixelsPerBlock, pixelsPerBlock);
                            pose.translate(-playerX, -playerZ);
                            for (ClaimCache.PackedRect claim : visible) {
                                graphics.fill(claim.minX(), claim.minZ(), claim.maxX() + 1, claim.maxZ() + 1, claim.argb());
                            }
                        } finally {
                            pose.popMatrix();
                        }
                    } finally {
                        graphics.disableScissor();
                    }
                }
            } else {
                int mx0 = context.x + pad, my0 = context.y + pad;
                int mx1 = context.x + context.w - pad, my1 = context.y + context.h - pad;
                graphics.enableScissor(mx0, my0, mx1, my1);
                try {
                    pose.pushMatrix();
                    try {
                        pose.translate(centerX, centerY);
                        pose.rotate(rot);
                        pose.scale(pixelsPerBlock, pixelsPerBlock);
                        pose.translate(-playerX, -playerZ);
                        for (ClaimCache.PackedRect claim : visible) {
                            graphics.fill(claim.minX(), claim.minZ(), claim.maxX() + 1, claim.maxZ() + 1, claim.argb());
                        }
                    } finally {
                        pose.popMatrix();
                    }
                } finally {
                    graphics.disableScissor();
                }
            }
        } catch (Exception exception) {
            if (!errorLogged) {
                errorLogged = true;
                FactionsMod.LOGGER.warn("Unable to render faction claims on Xaero's minimap", exception);
            }
        }
    }
}
