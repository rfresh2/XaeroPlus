package xaeroplus.feature.render.buffered;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector2f;
import org.joml.Vector3f;
import xaeroplus.module.impl.FpsLimiter;

import java.util.function.Supplier;

/**
 * Significant inspiration and code present has been adapted from: https://github.com/tr7zw/Exordium
 */
public class BufferedComponent {

    private static final Minecraft minecraft = Minecraft.getInstance();
    private static Model model = null;
    // the render state we're buffering original render output into
    private final RenderTarget renderTarget = new TextureTarget(100, 100, true, false);
    // for timing when the original render should be invoked
    private long fpsTimer = System.currentTimeMillis();
    private int guiScale = 0;
    private boolean isRendering = false;
    private boolean blendStateCaptured = false;
    private int srcRgb = 1;
    private int dstRgb = 0;
    private int srcAlpha = 1;
    private int dstAlpha = 0;
    private final Supplier<Integer> fpsLimitSupplier;

    public BufferedComponent(final Supplier<Integer> fpsLimitSupplier) {
        this.fpsLimitSupplier = fpsLimitSupplier;
    }

    private static void refreshModel(int screenWidth, int screenHeight) {
        if (model != null) model.close();
        var modelData = new Vector3f[] {
            new Vector3f(0.0f, screenHeight, -90.0f),
            new Vector3f(screenWidth, screenHeight, -90.0F),
            new Vector3f(screenWidth, 0.0F, -90.0F),
            new Vector3f(0.0F, 0.0F, -90.0F),
        };
        var uvData = new Vector2f[] {
            new Vector2f(0.0f, 0.0f),
            new Vector2f(1.0f, 0.0f),
            new Vector2f(1.0f, 1.0f),
            new Vector2f(0.0f, 1.0f),
        };
        model = new Model(modelData, uvData);
    }

    /**
     * @return true if the original render call should be cancelled
     */
    public boolean render() {
        if (!blendStateCaptured) return false;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean forceRender = false;
        if (renderTarget.width != minecraft.getWindow().getWidth()
            || renderTarget.height != minecraft.getWindow().getHeight()
            || minecraft.options.guiScale().get() != guiScale) {
            renderTarget.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), true);
            refreshModel(screenWidth, screenHeight);
            guiScale = minecraft.options.guiScale().get();
            forceRender = true;
        }
        if (model == null) refreshModel(screenWidth, screenHeight);
        boolean updateFrame = forceRender || System.currentTimeMillis() > fpsTimer;
        if (!updateFrame) {
            renderTextureOverlay(renderTarget.getColorTextureId());
            applyCapturedBlendState();
            return true;
        }
        renderTarget.setClearColor(0, 0, 0, 0);
        renderTarget.clear(false);
        renderTarget.bindWrite(false);
        isRendering = true;
        FpsLimiter.renderTargetOverwrite = renderTarget;
        correctBlendMode();
        renderTarget.bindWrite(false);
        return false;
    }

    public void postRender() {
        if (!blendStateCaptured) captureBlendState();
        if (!isRendering) return;
        FpsLimiter.renderTargetOverwrite = null;
        renderTarget.unbindWrite();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        fpsTimer = System.currentTimeMillis() + (1000 / fpsLimitSupplier.get());
        isRendering = false;
        renderTextureOverlay(renderTarget.getColorTextureId());
        applyCapturedBlendState();
    }

    private void renderTextureOverlay(int textureid) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, textureid);
        model.draw(RenderSystem.getModelViewMatrix());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void correctBlendMode() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    public void captureBlendState() {
        blendStateCaptured = true;
        srcRgb = GlStateManager.BLEND.srcRgb;
        srcAlpha = GlStateManager.BLEND.srcAlpha;
        dstRgb = GlStateManager.BLEND.dstRgb;
        dstAlpha = GlStateManager.BLEND.dstAlpha;
    }

    public void applyCapturedBlendState() {
        GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }
}
