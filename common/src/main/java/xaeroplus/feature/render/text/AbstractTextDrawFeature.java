package xaeroplus.feature.render.text;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

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
            var preparedText = font.prepareText(
                text.value(),
                0, 0,
                text.color(),
                true,
                0
            );
            preparedText.visit(new Font.GlyphVisitor() {
                @Override
                public void acceptRenderable(final TextRenderable renderable) {
                    RenderType renderType = XaeroPlusShaders.TEXT_NO_CULL;
                    VertexConsumer vertexConsumer = ctx.renderTypeBuffers().getBuffer(renderType);
                    renderable.render(textMatrix, vertexConsumer, 15728880, false);
                }
            });
        }
        if (!texts.isEmpty()) {
            ctx.renderTypeBuffers().endBatch();
        }
    }
}
