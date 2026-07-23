package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.HudMod;
import xaero.common.graphics.ImprovedFramebuffer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.MinimapLogs;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.compass.render.CompassRenderer;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.render.WaypointMapRenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.CustomMinimapFBORenderer;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

@Mixin(value = MinimapFBORenderer.class, remap = false)
public abstract class MixinMinimapFBORenderer extends MinimapRenderer implements CustomMinimapFBORenderer {

    @Shadow
    private ImprovedFramebuffer scalingFramebuffer;
    @Shadow
    private ImprovedFramebuffer rotationFramebuffer;
    @Shadow
    private boolean loadedFBO;

    public MixinMinimapFBORenderer(final HudMod modMain, final Minecraft mc, final WaypointMapRenderer waypointMapRenderer, final Minimap minimap, final CompassRenderer compassRenderer, final PoseStack matrixStack) {
        super(modMain, mc, waypointMapRenderer, minimap, compassRenderer, matrixStack);
    }

    @ModifyExpressionValue(
        method = "loadFrameBuffer",
        at = @At(
            value = "CONSTANT",
            args = "intValue=512"
        )
    )
    public int overrideFrameBufferSize(int size) {
        return Globals.minimapScaleMultiplier * 512;
    }

    @Override
    public void reloadMapFrameBuffers() {
        if (!BuiltInHudModules.MINIMAP.getCurrentSession().getProcessor().canUseFrameBuffer()) {
            MinimapLogs.LOGGER.info("FBO mode not supported! Using minimap safe mode.");
        } else {
            if (this.scalingFramebuffer != null)
                this.scalingFramebuffer.destroyBuffers();
            if (this.rotationFramebuffer != null)
                this.rotationFramebuffer.destroyBuffers();
            final int scaledSize = Globals.minimapScaleMultiplier * 512;
            this.scalingFramebuffer = new ImprovedFramebuffer(scaledSize, scaledSize, true);
            this.rotationFramebuffer = new ImprovedFramebuffer(scaledSize, scaledSize, true);
            this.loadedFBO = this.scalingFramebuffer.getColorTexture() != null;
        }
    }

    @ModifyArg(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderChunksToFBO(Lxaero/hud/minimap/module/MinimapSession;Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;DIFIZZIDDZLxaero/lib/client/graphics/XaeroBufferProvider;)V"
    ),
        index = 6,
        remap = true) // $REMAP
    public int modifyViewW(final int viewW) {
        return viewW * Globals.minimapScaleMultiplier;
    }

    @Inject(method = "renderChunksToFBO", at = @At(
        value = "HEAD"
    ), remap = true)
    public void modifyScaledSize(
        final CallbackInfo ci,
        @Share("scaledSize") LocalIntRef scaledSize
    ) {
        int s = 256 * Globals.minimapScaleMultiplier * Globals.minimapSizeMultiplier;
        if (Globals.minimapSizeMultiplier > 1) {
            int f = (Globals.minimapSizeMultiplier - 1) * Globals.minimapScaleMultiplier;
            s -= f * 6;
            int scaledMinimapSize = modMain.getHudConfigs().getClientConfigManager().getEffective(
                MinimapProfiledConfigOptions.SIZE);
            int minimapNormalSize = scaledMinimapSize / Globals.minimapSizeMultiplier;
            int minimapScaledSizeDiff = 250 - minimapNormalSize;
            s -= minimapScaledSizeDiff * f;
        }
        scaledSize.set(s);
    }

    @Redirect(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lorg/joml/Matrix4fStack;translate(FFF)Lorg/joml/Matrix4f;",
        ordinal = 0
    ), remap = true) // $REMAP
    public Matrix4f modifyShaderMatrixStackTranslate(final Matrix4fStack instance, final float x, final float y, final float z,
                                                     @Share("scaledSize") LocalIntRef scaledSize) {
        float translate = 256.0f * Globals.minimapScaleMultiplier;
        return instance.translate(translate, translate, -2000.0F);
    }

    @ModifyArg(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapRendererHelper;addColoredLineToExistingBuffer(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFF)V"
    ),
        index = 10
    )
    public float modifyChunkGridLineWidth(final float lineWidth) {
        return Math.max(1.0f, lineWidth * Globals.minimapScaleMultiplier / (float) Globals.minimapSizeMultiplier);
    }

    @Redirect(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lorg/joml/Matrix4fStack;translate(FFF)Lorg/joml/Matrix4f;",
        ordinal = 0
    ),
        slice = @Slice(
        from = @At(
            value = "INVOKE",
            target = "Lxaero/common/graphics/ImprovedFramebuffer;getTas()Lnet/minecraft/client/renderer/rendertype/RenderSetup$TextureAndSampler;"
        )
    ), remap = true)
    public Matrix4f correctPreRotationTranslationForSizeMult(final Matrix4fStack instance, final float x, final float y, final float z) {
        return instance.translate((x / Globals.minimapSizeMultiplier), (y / Globals.minimapSizeMultiplier), z);
    }

    @WrapOperation(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/lib/client/graphics/util/ImmediateRenderUtil;texturedRect(Lcom/mojang/blaze3d/vertex/PoseStack;FFIIFFFFLcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/rendertype/RenderSetup$TextureAndSampler;)V",
        ordinal = 0
    ), slice = @Slice(
        from = @At(
            value = "INVOKE",
            target = "Lxaero/common/graphics/ImprovedFramebuffer;bindAsMainTarget(Z)V",
            ordinal = 1
        )
    ))
    public void correctScaledFBO(final PoseStack matrixStack, final float x, final float y, final int textureX, final int textureY, final float width, final float height, final float textureH, final float factor, final RenderPipeline renderPipeline, final RenderSetup.TextureAndSampler texture, final Operation<Void> original) {
        original.call(
            matrixStack,
            x * Globals.minimapScaleMultiplier,
            y * Globals.minimapScaleMultiplier,
            textureX,
            textureY,
            width * Globals.minimapScaleMultiplier,
            height * Globals.minimapScaleMultiplier,
            textureH * Globals.minimapScaleMultiplier,
            factor * Globals.minimapScaleMultiplier,
            renderPipeline,
            texture
        );
    }

    @WrapOperation(method = "renderChunksToFBO", at= @At(
        value = "INVOKE",
        target = "Lxaero/common/mods/SupportXaeroWorldmap;drawMinimap(Lxaero/hud/minimap/module/MinimapSession;Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;IIIIIIZDDLcom/mojang/blaze3d/vertex/VertexConsumer;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V"),
        remap = true) // $REMAP
    public void drawMinimapFeatures(final SupportXaeroWorldmap instance, final MinimapSession minimapSession, final PoseStack matrixStack, final MinimapRendererHelper helper, final int xFloored, final int zFloored, final int minViewX, final int minViewZ, final int maxViewX, final int maxViewZ, final boolean zooming, final double zoom, final double mapDimensionScale, final VertexConsumer overlayBufferBuilder, final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, final Operation<Void> original,
                                    @Local(name = "renderTypeBuffers") XaeroBufferProvider renderTypeBuffers
    ) {
        original.call(instance, minimapSession, matrixStack, helper, xFloored, zFloored, minViewX, minViewZ, maxViewX, maxViewZ, zooming, zoom, mapDimensionScale, overlayBufferBuilder, multiTextureRenderTypeRenderers);
        int mapX = xFloored >> 4;
        int mapZ = zFloored >> 4;
        int chunkX = mapX >> 2;
        int chunkZ = mapZ >> 2;
        int tileX = mapX & 3;
        int tileZ = mapZ & 3;
        int insideX = xFloored & 15;
        int insideZ = zFloored & 15;
        XaeroPlusShaders.setFrameSize((float)this.scalingFramebuffer.width, (float)this.scalingFramebuffer.height);
        Globals.drawManager.drawMinimapFeatures(
            chunkX,
            chunkZ,
            tileX,
            tileZ,
            insideX,
            insideZ,
            zoom,
            matrixStack,
            renderTypeBuffers
        );
    }

    @WrapOperation(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;draw(Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V"
    ))
    public void drawMinimapFeaturesCaveMode(final MultiTextureRenderTypeRendererProvider instance, final MultiTextureRenderTypeRenderer renderer, final Operation<Void> original,
                                            @Local(name = "xFloored") int xFloored,
                                            @Local(name = "zFloored") int zFloored,
                                            @Local(name = "matrixStack") PoseStack matrixStack,
                                            @Local(name = "renderTypeBuffers") XaeroBufferProvider renderTypeBuffers
    ) {
        original.call(instance, renderer);
        XaeroPlusShaders.setFrameSize((float)this.scalingFramebuffer.width, (float)this.scalingFramebuffer.height);
        int mapX = xFloored >> 4;
        int mapZ = zFloored >> 4;
        int chunkX = mapX >> 2;
        int chunkZ = mapZ >> 2;
        int tileX = mapX & 3;
        int tileZ = mapZ & 3;
        int insideX = xFloored & 15;
        int insideZ = zFloored & 15;
        Globals.drawManager.drawMinimapFeatures(
            chunkX,
            chunkZ,
            tileX,
            tileZ,
            insideX,
            insideZ,
            zoom,
            matrixStack,
            renderTypeBuffers
        );
    }
}
