package xaeroplus.feature.render.buffered;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

import java.util.function.IntSupplier;

/**
 * Significant inspiration and code present has been adapted from: https://github.com/tr7zw/Exordium
 */
public class BufferedComponent {
    private static final Minecraft mc = Minecraft.getInstance();
    private final RenderTarget renderTarget = new TextureTarget("XaeroPlus Minimap Buffered", 100, 100, true);
    private RenderTarget mainRenderTargetBackup = null;
    private long nextRenderCapture = System.currentTimeMillis();
    private final IntSupplier fpsLimitSupplier;

    public BufferedComponent(final IntSupplier fpsLimitSupplier) {
        this.fpsLimitSupplier = fpsLimitSupplier;
    }

    /**
     * @return true if the original render call should be cancelled
     */
    public boolean render() {
        var forceRender = false;
        if (renderTarget.width != mc.mainRenderTarget.width
            || renderTarget.height != mc.mainRenderTarget.height
        ) {
            renderTarget.resize(mc.mainRenderTarget.width, mc.mainRenderTarget.height);
            forceRender = true;
        }
        if (forceRender || System.currentTimeMillis() > nextRenderCapture) {
            mainRenderTargetBackup = mc.mainRenderTarget;
            mc.mainRenderTarget = renderTarget;
            clearRenderTarget(renderTarget, 0, 1.0F);
            return false;
        }
        renderBufferedTexture();
        return true;
    }

    public static void clearRenderTarget(RenderTarget renderTarget, int color, float depth) {
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(
                renderTarget.getColorTexture(),
                color,
                renderTarget.getDepthTexture(),
                depth
            );
    }

    public void postRender() {
        if (mainRenderTargetBackup != null) {
            mc.mainRenderTarget = mainRenderTargetBackup;
            mainRenderTargetBackup = null;
        }
        nextRenderCapture = System.currentTimeMillis() + (1000 / fpsLimitSupplier.getAsInt());
        renderBufferedTexture();
    }

    private void renderBufferedTexture() {
        renderTarget.blitAndBlendToTexture(mc.getMainRenderTarget().getColorTextureView());
    }
}
