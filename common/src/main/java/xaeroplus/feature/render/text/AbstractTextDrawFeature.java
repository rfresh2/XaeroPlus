package xaeroplus.feature.render.text;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
            ctx.matrixStack().pushPose();
            float textScale = text.scale() * 2.0f * (float) Mth.clamp(
                (ctx.worldmap() ? 1f : Globals.minimapScaleMultiplier) / ctx.fboScale(),
                0.1f * (ctx.worldmap() ? 1.0f : Globals.minimapScaleMultiplier),
                1000f
            );
            float width = font.width(text.value());
            ctx.matrixStack().scale(textScale, textScale, 1);
            ctx.matrixStack().translate(
                text.x() / textScale,
                text.z() / textScale,
                0
            );
            ctx.matrixStack().translate(
                -width / 2,
                -font.lineHeight / 2.0f,
                0
            );
            font.drawInBatch(
                text.value(),
                0,
                0,
                text.color(),
                true,
                ctx.matrixStack().last().pose(),
                ctx.renderTypeBuffers(),
                Font.DisplayMode.NORMAL,
                0,
                15728880,
                font.isBidirectional()
            );
            ctx.matrixStack().popPose();
        }
        if (!texts.isEmpty()) {
            RenderSystem.disableCull();
            ctx.renderTypeBuffers().endLastBatch();
        }
    }
}
