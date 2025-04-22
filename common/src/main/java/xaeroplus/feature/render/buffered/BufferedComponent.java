package xaeroplus.feature.render.buffered;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import xaero.common.graphics.TextureUtils;
import xaeroplus.module.impl.FpsLimiter;
import xaeroplus.settings.Settings;

import java.util.OptionalInt;
import java.util.function.IntSupplier;

/**
 * Significant inspiration and code present has been adapted from: https://github.com/tr7zw/Exordium
 */
public class BufferedComponent {
    private static final Minecraft mc = Minecraft.getInstance();
    private Model model = null;
    private final RenderTarget renderTarget = new TextureTarget("XaeroPlus Minimap Buffered", 100, 100, true);
    private long nextRenderCapture = System.currentTimeMillis();
    private final IntSupplier fpsLimitSupplier;
    private final Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
    private final RenderPipeline bufferedPipeline = RenderPipeline.builder()
        .withLocation(ResourceLocation.fromNamespaceAndPath("xaeroplus", "buffered"))
        .withVertexShader("core/position_tex")
        .withFragmentShader("core/position_tex")
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withUniform("ModelViewMat", UniformType.MATRIX4X4)
        .withUniform("ProjMat", UniformType.MATRIX4X4)
        .withUniform("ColorModulator", UniformType.VEC4)
        .withSampler("Sampler0")
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .build();

    public BufferedComponent(final IntSupplier fpsLimitSupplier) {
        this.fpsLimitSupplier = fpsLimitSupplier;
    }

    private void refreshModel(final int screenWidth, final int screenHeight) {
        if (model != null) model.close();
        var posMatrix = new Vector3f[] {
            new Vector3f(0.0f, screenHeight, 1f),
            new Vector3f(screenWidth, screenHeight, 1f),
            new Vector3f(screenWidth, 0.0F, 1f),
            new Vector3f(0.0F, 0.0F, 1f),
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
            || renderTarget.height != windowHeight
        ) {
            renderTarget.resize(windowWidth, windowHeight);
            refreshModel(windowWidth, windowHeight);
            forceRender = true;
        }
        if (model == null) {
            refreshModel(windowWidth, windowHeight);
            forceRender = true;
        }
        // todo:
        if (forceRender || System.currentTimeMillis() > nextRenderCapture) {
            TextureUtils.clearRenderTarget(renderTarget, 0, 1.0F);
            // todo
//            renderTarget.bindWrite(false);
            FpsLimiter.renderTargetOverwrite = renderTarget;
//            RenderSystem.enableBlend();
//            RenderSystem.blendFuncSeparate(
//                GlStateManager.SourceFactor.SRC_ALPHA,
//                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
//                GlStateManager.SourceFactor.ONE,
//                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
//            );
//            renderTarget.bindWrite(false);
            return false;
        }
        renderBufferedTexture(renderTarget.getColorTexture());
        return true;
    }

    public void postRender() {
        FpsLimiter.renderTargetOverwrite = null;
        // todo:
//        renderTarget.unbindWrite();
//        mc.getMainRenderTarget().bindWrite(true);
        nextRenderCapture = System.currentTimeMillis() + (1000 / fpsLimitSupplier.getAsInt());
        renderBufferedTexture(renderTarget.getColorTexture());
    }

    private void renderBufferedTexture(final GpuTexture textureId) {
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(mc.getMainRenderTarget().getColorTexture(), OptionalInt.empty())) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, textureId);
            modelViewMatrix.set(RenderSystem.getModelViewMatrix());
            modelViewMatrix.translate(0, 0, 399 + (float) Settings.REGISTRY.minimapRenderZOffsetSetting.get());
            var guiScale = (float) Math.max(1.0, mc.getWindow().getGuiScale());
            modelViewMatrix.scale(1.0f / guiScale);
            pass.setPipeline(bufferedPipeline);
            pass.setUniform("ModelViewMat", modelViewMatrix);
            pass.setUniform("ProjMat", RenderSystem.getProjectionMatrix());
            model.draw(pass);
        }
    }
}
