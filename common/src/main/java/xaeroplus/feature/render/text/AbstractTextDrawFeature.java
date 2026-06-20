package xaeroplus.feature.render.text;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import xaero.lib.client.graphics.font.util.FontUtils;
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
            FontUtils.drawText(
                font,
                ctx.matrixStack(),
                text.value(),
                0, 0,
                text.color(),
                true,
                Font.DisplayMode.NORMAL,
                ctx.renderTypeBuffers()
            );
            ctx.matrixStack().popPose();
        }
        if (!texts.isEmpty()) {
            try {
                Globals.disableDrawCullingOverride = true;
                ctx.renderTypeBuffers().endBatch();
            } finally {
                Globals.disableDrawCullingOverride = false;
            }
        }
    }
}
