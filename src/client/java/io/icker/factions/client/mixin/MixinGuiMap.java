package io.icker.factions.client.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.client.ClaimCache;
import io.icker.factions.client.ClaimCache.PackedRect;
import io.icker.factions.client.ClaimOverlayToggle;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public class MixinGuiMap {
    @Shadow private double cameraX;
    @Shadow private double cameraZ;
    @Shadow private double scale;
    @Shadow private double screenScale;
    @Shadow private xaero.map.MapProcessor mapProcessor;

    private static boolean factions$warned;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void factions$renderClaims(
            GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            if (!ClaimOverlayToggle.isWorldMapEnabled()) return;
            if (mapProcessor == null || scale <= 0.0 || screenScale <= 0.0) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            String dimId = mc.level.dimension().identifier().toString();
            List<PackedRect> claims = ClaimCache.getForDim(dimId);
            if (claims.isEmpty()) return;

            double pixelsPerBlock = scale / screenScale;
            if (pixelsPerBlock * 16.0 < 0.5) return;
            double centerX = extractor.guiWidth() * 0.5;
            double centerZ = extractor.guiHeight() * 0.5;
            double viewMinX = cameraX - centerX / pixelsPerBlock - 16.0;
            double viewMaxX = cameraX + centerX / pixelsPerBlock + 16.0;
            double viewMinZ = cameraZ - centerZ / pixelsPerBlock - 16.0;
            double viewMaxZ = cameraZ + centerZ / pixelsPerBlock + 16.0;
            for (PackedRect claim : claims) {
                if (claim.maxX() + 1 < viewMinX || claim.minX() > viewMaxX) continue;
                if (claim.maxZ() + 1 < viewMinZ || claim.minZ() > viewMaxZ) continue;
                int minX = (int) Math.floor(centerX + (claim.minX() - cameraX) * pixelsPerBlock);
                int minZ = (int) Math.floor(centerZ + (claim.minZ() - cameraZ) * pixelsPerBlock);
                int maxX = (int) Math.ceil(centerX + (claim.maxX() + 1 - cameraX) * pixelsPerBlock);
                int maxZ = (int) Math.ceil(centerZ + (claim.maxZ() + 1 - cameraZ) * pixelsPerBlock);
                extractor.fill(minX, minZ, maxX, maxZ, claim.argb());
            }
        } catch (RuntimeException exception) {
            if (!factions$warned) {
                factions$warned = true;
                FactionsMod.LOGGER.warn("Unable to render faction claims on Xaero's World Map", exception);
            }
        }
    }
}
