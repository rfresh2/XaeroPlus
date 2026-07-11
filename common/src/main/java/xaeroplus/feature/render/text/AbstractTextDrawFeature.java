package xaeroplus.feature.render.text;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;

import java.util.Collection;

public abstract class AbstractTextDrawFeature implements DrawFeature {

    public abstract Collection<Text> getTexts();

    @Override
    public void render(final DrawContext ctx) {
        var font = Minecraft.getInstance().font;
        var texts = getTexts();
        for (var text : texts) {
            float xpMinimapScalar = (float) Globals.minimapScaleMultiplier / (float) Globals.minimapSizeMultiplier;
            float textScale = text.scale() * 2.0f * (float) Mth.clamp(
                (ctx.worldmap() ? 1f : xpMinimapScalar) / ctx.fboScale(),
                0.1f * (ctx.worldmap() ? 1.0f : xpMinimapScalar),
                1000f
            );
            float width = font.width(text.value());
            var relativeX = (float) ((long) text.x() - ctx.cameraBlockX());
            var relativeZ = (float) ((long) text.z() - ctx.cameraBlockZ());
            var textMatrix = new Matrix4f(ctx.untranslatedMapViewMatrix())
                .translate(relativeX, relativeZ, 0.0f)
                .scale(textScale, textScale, 1.0f)
                .translate(-width / 2.0f, -font.lineHeight / 2.0f, 0.0f);
            font.drawInBatch(
                text.value(),
                0,
                0,
                text.color(),
                true,
                textMatrix,
                ctx.renderTypeBuffers(),
                Font.DisplayMode.NORMAL,
                0,
                15728880
            );
        }
        if (!texts.isEmpty()) {
            try {
                Globals.disableDrawCullingOverride = true;
                ctx.renderTypeBuffers().endLastBatch();
            } finally {
                Globals.disableDrawCullingOverride = false;
            }
        }
    }
}
