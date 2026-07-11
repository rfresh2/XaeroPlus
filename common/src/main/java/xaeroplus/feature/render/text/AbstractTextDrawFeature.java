package xaeroplus.feature.render.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
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
            var textMatrix = new Matrix4f(ctx.untranslatedMapViewMatrix());
            textMatrix.translate(new Vector3f(relativeX, relativeZ, 0.0f));
            textMatrix.multiply(Matrix4f.createScaleMatrix(textScale, textScale, 1.0f));
            textMatrix.translate(new Vector3f(-width / 2.0f, -font.lineHeight / 2.0f, 0.0f));
            font.drawInBatch(
                text.value(),
                0,
                0,
                text.color(),
                false,
                textMatrix,
                ctx.renderTypeBuffers(),
                false,
                0,
                15728880,
                font.isBidirectional()
            );
        }
        if (!texts.isEmpty()) {
            RenderSystem.disableCull();
            ctx.renderTypeBuffers().endLastBatch();
        }
    }
}
