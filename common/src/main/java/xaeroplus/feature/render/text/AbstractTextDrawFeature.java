package xaeroplus.feature.render.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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
            var textMatrixStack = new PoseStack();
            textMatrixStack.pushPose();
            textMatrixStack.last().pose().load(ctx.untranslatedMapViewMatrix());
            textMatrixStack.translate(relativeX, relativeZ, 0.0f);
            textMatrixStack.scale(textScale, textScale, 1.0f);
            textMatrixStack.translate(-width / 2.0f, -font.lineHeight / 2.0f, 0.0f);
            font.drawInBatch(
                text.value(),
                0,
                0,
                text.color(),
                false,
                textMatrixStack.last().pose(),
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
