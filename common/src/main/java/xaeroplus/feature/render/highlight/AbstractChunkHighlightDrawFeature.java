package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

public abstract class AbstractChunkHighlightDrawFeature implements DrawFeature {
    public final AbstractHighlightVertexBuffer drawBuffer;

    protected AbstractChunkHighlightDrawFeature(final AbstractHighlightVertexBuffer drawBuffer) {
        this.drawBuffer = drawBuffer;
    }

    public void preRender(final DrawContext ctx) {
        XaeroPlusShaders.ensureShaders();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
    }

    public void postRender(final DrawContext ctx) {
        RenderSystem.disableBlend();
    }

    @Override
    public void invalidateCache() {
        drawBuffer.markStale();
    }

    @Override
    public void close() {
        drawBuffer.close();
    }

}
