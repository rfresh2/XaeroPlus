package xaeroplus.feature.render.buffered;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import xaeroplus.module.impl.FpsLimiter;

import java.util.function.IntSupplier;

/**
 * Significant inspiration and code present has been adapted from: https://github.com/tr7zw/Exordium
 */
public class BufferedComponent {
    private static final Minecraft mc = Minecraft.getInstance();
    private Model model = null;
    private final RenderTarget renderTarget = new TextureTarget(100, 100, true, false);
    private long nextRenderCapture = System.currentTimeMillis();
    private final IntSupplier fpsLimitSupplier;
    private final Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());

    public BufferedComponent(final IntSupplier fpsLimitSupplier) {
        this.fpsLimitSupplier = fpsLimitSupplier;
    }

    private void refreshModel(final int screenWidth, final int screenHeight) {
        if (model != null) model.close();
        var posMatrix = new Vector3f[] {
            new Vector3f(0.0f, screenHeight, -90.0f),
            new Vector3f(screenWidth, screenHeight, -90.0F),
            new Vector3f(screenWidth, 0.0F, -90.0F),
            new Vector3f(0.0F, 0.0F, -90.0F),
        };
        var texUvMatrix = new Vector2f[] {
            new Vector2f(0.0f, 0.0f),
            new Vector2f(1.0f, 0.0f),
            new Vector2f(1.0f, 1.0f),
            new Vector2f(0.0f, 1.0f),
        };
        model = new Model(posMatrix, texUvMatrix);
    }

    /**
     * @return true if the original render call should be cancelled
     */
    public boolean render() {
        var windowWidth = mc.getWindow().getWidth();
        var windowHeight = mc.getWindow().getHeight();
        var forceRender = false;
        if (renderTarget.width != windowWidth
            || renderTarget.height != windowHeight) {
            renderTarget.resize(windowWidth, windowHeight, true);
            refreshModel(windowWidth, windowHeight);
            forceRender = true;
        }
        if (model == null) {
            refreshModel(windowWidth, windowHeight);
            forceRender = true;
        }
        if (forceRender || System.currentTimeMillis() > nextRenderCapture) {
            renderTarget.setClearColor(0, 0, 0, 0);
            renderTarget.clear(false);
            renderTarget.bindWrite(false);
            FpsLimiter.renderTargetOverwrite = renderTarget;
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            renderTarget.bindWrite(false);
            return false;
        }
        renderBufferedTexture(renderTarget.getColorTextureId());
        return true;
    }

    public void postRender() {
        FpsLimiter.renderTargetOverwrite = null;
        renderTarget.unbindWrite();
        mc.getMainRenderTarget().bindWrite(true);
        nextRenderCapture = System.currentTimeMillis() + (1000 / fpsLimitSupplier.getAsInt());
        renderBufferedTexture(renderTarget.getColorTextureId());
    }

    private void renderBufferedTexture(final int textureId) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, textureId);
        modelViewMatrix.set(RenderSystem.getModelViewMatrix());
        var guiScale = (float) Math.max(1.0, mc.getWindow().getGuiScale());
        modelViewMatrix.scale(1.0f / guiScale);
        model.draw(modelViewMatrix);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
