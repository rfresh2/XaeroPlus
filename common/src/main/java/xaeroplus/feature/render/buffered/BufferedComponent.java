package xaeroplus.feature.render.buffered;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import xaeroplus.module.impl.FpsLimiter;

import java.util.function.Supplier;

import static xaeroplus.feature.render.buffered.Model.Vector2f;

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
    private final Supplier<Integer> fpsLimitSupplier;
    private final Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());

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
        int windowWidth = minecraft.getWindow().getWidth();
        int windowHeight = minecraft.getWindow().getHeight();
        boolean forceRender = false;
        if (renderTarget.width != windowWidth
            || renderTarget.height != windowHeight) {
            renderTarget.resize(windowWidth, windowHeight, true);
            refreshModel(windowWidth, windowHeight);
            forceRender = true;
        }
        if (model == null) refreshModel(windowWidth, windowHeight);
        boolean updateFrame = forceRender || System.currentTimeMillis() > fpsTimer;
        if (!updateFrame) {
            renderTextureOverlay(renderTarget.getColorTextureId());
            return true;
        }
        renderTarget.setClearColor(0, 0, 0, 0);
        renderTarget.clear(false);
        renderTarget.bindWrite(false);
        isRendering = true;
        FpsLimiter.renderTargetOverwrite = renderTarget;
        applyBlend();
        renderTarget.bindWrite(false);
        return false;
    }

    public void postRender() {
        if (!isRendering) return;
        FpsLimiter.renderTargetOverwrite = null;
        renderTarget.unbindWrite();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        fpsTimer = System.currentTimeMillis() + (1000 / fpsLimitSupplier.get());
        isRendering = false;
        renderTextureOverlay(renderTarget.getColorTextureId());
    }

    private void renderTextureOverlay(int textureid) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, textureid);
        modelViewMatrix.load(RenderSystem.getModelViewMatrix());
        var guiScale = (float) Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale());
        float scalar = 1.0f / guiScale;
        Matrix4f scaleMatrix = Matrix4f.createScaleMatrix(scalar, scalar, scalar);
        modelViewMatrix.multiply(scaleMatrix);
        model.draw(modelViewMatrix);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void applyBlend() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }
}
