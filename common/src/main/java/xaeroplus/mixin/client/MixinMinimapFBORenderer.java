package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.graphics.ImprovedFramebuffer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.graphics.shader.FramebufferLinesShaderHelper;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.MinimapLogs;
import xaero.hud.minimap.compass.render.CompassRenderer;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.render.WaypointMapRenderer;
import xaero.hud.render.util.ImmediateRenderUtil;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.CustomMinimapFBORenderer;
import xaeroplus.settings.Settings;
import xaeroplus.util.ColorHelper;

@Mixin(value = MinimapFBORenderer.class, remap = false)
public abstract class MixinMinimapFBORenderer extends MinimapRenderer implements CustomMinimapFBORenderer {

    @Shadow
    private ImprovedFramebuffer scalingFramebuffer;
    @Shadow
    private ImprovedFramebuffer rotationFramebuffer;
    @Shadow
    private boolean loadedFBO;

    public MixinMinimapFBORenderer(final IXaeroMinimap modMain, final Minecraft mc, final WaypointMapRenderer waypointsGuiRenderer, final Minimap minimap, final CompassRenderer compassRenderer) {
        super(modMain, mc, waypointsGuiRenderer, minimap, compassRenderer);
    }

    @ModifyConstant(method = "loadFrameBuffer", constant = @Constant(intValue = 512))
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
            this.rotationFramebuffer.setFilterMode(FilterMode.LINEAR);
            this.loadedFBO = this.scalingFramebuffer.getColorTexture() != null;
        }
    }

    @ModifyArg(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderChunksToFBO(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;DIFIZZIDDZLxaero/common/graphics/CustomVertexConsumers;)V"
    ),
        index = 6,
        remap = true) // $REMAP
    public int modifyViewW(final int viewW) {
        return viewW * Globals.minimapScaleMultiplier;
    }

    @Inject(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphics;pose()Lcom/mojang/blaze3d/vertex/PoseStack;"
    ), remap = true)
    public void modifyScaledSize(
        final CallbackInfo ci,
        @Share("scaledSize") LocalIntRef scaledSize
    ) {
        int s = 256 * Globals.minimapScaleMultiplier * Globals.minimapSizeMultiplier;
        if (Globals.minimapSizeMultiplier > 1) {
            int f = (Globals.minimapSizeMultiplier - 1) * Globals.minimapScaleMultiplier;
            s -= f * 6;
            int scaledMinimapSize = modMain.getSettings().getMinimapSize();
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

    @Redirect(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapRendererHelper;drawMyColoredRect(Lorg/joml/Matrix4f;FFFFI)V"
    ), remap = true)
    public void modifyMMBackgroundFill(final MinimapRendererHelper instance, final Matrix4f matrix, final float x1, final float y1, final float x2, final float y2, final int color, @Share("scaledSize") LocalIntRef scaledSize) {
        if (!Settings.REGISTRY.transparentMinimapBackground.get())
            instance.drawMyColoredRect(matrix, -scaledSize.get(), -scaledSize.get(), scaledSize.get(), scaledSize.get(), ColorHelper.getColor(0, 0, 0, 255));
        else
            instance.drawMyColoredRect(matrix, -scaledSize.get(), -scaledSize.get(), scaledSize.get(), scaledSize.get(), ColorHelper.getColor(0, 0, 0, 0));
    }

    @ModifyArg(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/blaze3d/systems/RenderSystem;lineWidth(F)V"
    ), remap = false)
    public float modifyChunkGridLineWidth(final float original) {
        return Math.max(1.0f, original * Globals.minimapScaleMultiplier / (float) Globals.minimapSizeMultiplier);
    }

    @Redirect(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lorg/joml/Matrix4fStack;translate(FFF)Lorg/joml/Matrix4f;",
        ordinal = 0
    ),
        slice = @Slice(
        from = @At(
            value = "INVOKE",
            target = "Lxaero/common/graphics/ImprovedFramebuffer;bindRead()V"
        )
    ), remap = true)
    public Matrix4f correctPreRotationTranslationForSizeMult(final Matrix4fStack instance, final float x, final float y, final float z) {
        return instance.translate(x / Globals.minimapSizeMultiplier, y / Globals.minimapSizeMultiplier, z);
    }

    @Inject(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lorg/joml/Matrix4fStack;translate(FFF)Lorg/joml/Matrix4f;",
        ordinal = 1,
        shift = At.Shift.BEFORE
    ), slice = @Slice(
        from = @At(
            value = "INVOKE",
            target = "Lxaero/common/graphics/ImprovedFramebuffer;bindRead()V"
        )
    ), remap = true)
    public void correctPostRotationTranslationForSizeMult(
        final CallbackInfo ci,
        @Local(name = "halfWView") float halfWView,
        @Local(name = "shaderMatrixStack") Matrix4fStack shaderMatrixStack
    ) {
        float sizeMultTranslation = (halfWView / Globals.minimapSizeMultiplier) * (Globals.minimapSizeMultiplier - 1);
        shaderMatrixStack.translate(sizeMultTranslation, sizeMultTranslation, 0f);
    }

    @Redirect(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/render/util/ImmediateRenderUtil;texturedRect(Lcom/mojang/blaze3d/vertex/PoseStack;FFIIFFFFLcom/mojang/blaze3d/pipeline/RenderPipeline;)V"
    ), remap = true) // $REMAP
    public void redirectModelViewDraw(final PoseStack matrixStack, final float x, final float y, final int textureX, final int textureY, final float width, final float height, final float textureH, final float factor, final RenderPipeline renderPipeline,
                                      @Share("scaledSize") LocalIntRef scaledSize) {
        final float scaledSizeM = Globals.minimapScaleMultiplier * 512f;
        ImmediateRenderUtil.texturedRect(matrixStack, -scaledSize.get(), -scaledSize.get(), 0, 0, scaledSizeM, scaledSizeM, scaledSizeM, scaledSizeM, renderPipeline);
    }

    @WrapOperation(method = "renderChunksToFBO", at= @At(
        value = "INVOKE",
        target = "Lxaero/common/mods/SupportXaeroWorldmap;drawMinimap(Lxaero/hud/minimap/module/MinimapSession;Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;IIIIIIZDDLcom/mojang/blaze3d/vertex/VertexConsumer;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V"),
        remap = true) // $REMAP
    public void drawMinimapFeatures(final SupportXaeroWorldmap instance, final MinimapSession minimapSession, final PoseStack matrixStack, final MinimapRendererHelper helper, final int xFloored, final int zFloored, final int minViewX, final int minViewZ, final int maxViewX, final int maxViewZ, final boolean zooming, final double zoom, final double mapDimensionScale, final VertexConsumer overlayBufferBuilder, final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, final Operation<Void> original,
                                    @Local(name = "renderTypeBuffers") MultiBufferSource.BufferSource renderTypeBuffers
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
        FramebufferLinesShaderHelper.setFrameSize((float)this.scalingFramebuffer.viewWidth, (float)this.scalingFramebuffer.viewHeight);
        Globals.drawManager.drawMinimapFeatures(
            chunkX,
            chunkZ,
            tileX,
            tileZ,
            insideX,
            insideZ,
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
                                            @Local(name = "renderTypeBuffers") MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        original.call(instance, renderer);
        FramebufferLinesShaderHelper.setFrameSize((float)this.scalingFramebuffer.viewWidth, (float)this.scalingFramebuffer.viewHeight);
        int mapX = xFloored >> 4;
        int mapZ = zFloored >> 4;
        int chunkX = mapX >> 2;
        int chunkZ = mapZ >> 2;
        int tileX = mapX & 3;
        int tileZ = mapZ & 3;
        int insideX = xFloored & 15;
        int insideZ = zFloored & 15;
        Globals.drawManager.drawMinimapFeatures(
            chunkX, chunkZ,
            tileX, tileZ,
            insideX, insideZ,
            matrixStack,
            renderTypeBuffers
        );
    }
}
