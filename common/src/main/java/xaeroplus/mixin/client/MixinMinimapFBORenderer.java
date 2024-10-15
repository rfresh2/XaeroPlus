package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.graphics.CustomRenderTypes;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.graphics.ImprovedFramebuffer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.graphics.shader.MinimapShaders;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.render.WaypointsGuiRenderer;
import xaero.common.misc.OptimizedMath;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.MinimapLogs;
import xaero.hud.minimap.compass.render.CompassRenderer;
import xaero.hud.minimap.module.MinimapSession;
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

    public MixinMinimapFBORenderer(final IXaeroMinimap modMain, final Minecraft mc, final WaypointsGuiRenderer waypointsGuiRenderer, final Minimap minimap, final CompassRenderer compassRenderer) {
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
            this.scalingFramebuffer = new ImprovedFramebuffer(scaledSize, scaledSize, false);
            this.rotationFramebuffer = new ImprovedFramebuffer(scaledSize, scaledSize, true);
            this.rotationFramebuffer.setFilterMode(9729);
            this.loadedFBO = this.scalingFramebuffer.frameBufferId != -1 && this.rotationFramebuffer.frameBufferId != -1;
        }
    }

    @ModifyArg(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderChunksToFBO(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;DDIIFFIZZZIDDZZLxaero/common/graphics/CustomVertexConsumers;)V"
    ),
        index = 9,
        remap = true) // $REMAP
    public int modifyViewW(final int viewW) {
        return viewW * Globals.minimapScaleMultiplier;
    }

    @Inject(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphics;pose()Lcom/mojang/blaze3d/vertex/PoseStack;"
    ), remap = true)
    public void modifyScaledSize(final MinimapSession minimapSession, final GuiGraphics guiGraphics, final MinimapProcessor minimap, final Player player, final Entity renderEntity, final Vec3 renderPos, final double playerDimDiv, final double mapDimensionScale, final int bufferSize, final int viewW, final float sizeFix, final float partial, final int level, final boolean retryIfError, final boolean useWorldMap, final boolean lockedNorth, final int shape, final double ps, final double pc, final boolean cave, final boolean circle, final CustomVertexConsumers cvc, final CallbackInfo ci,
                                 @Share("scaledSize") LocalIntRef scaledSize) {
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
        target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
    ), remap = true)
    public void modifyMMBackgroundFill(final GuiGraphics guiGraphics, final int x1, final int y1, final int x2, final int y2, final int color,
                                       @Share("scaledSize") LocalIntRef scaledSize) {
        if (!Settings.REGISTRY.transparentMinimapBackground.get())
            guiGraphics.fill(-scaledSize.get(), -scaledSize.get(), scaledSize.get(), scaledSize.get(), ColorHelper.getColor(0, 0, 0, 255));
        else
            guiGraphics.fill(-scaledSize.get(), -scaledSize.get(), scaledSize.get(), scaledSize.get(), ColorHelper.getColor(0, 0, 0, 0));
    }

    @ModifyArg(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/blaze3d/systems/RenderSystem;lineWidth(F)V"
    ), remap = false)
    public float modifyChunkGridLineWidth(final float original) {
        return Math.max(1.0f, original * Globals.minimapScaleMultiplier / (float) Globals.minimapSizeMultiplier);
    }

    @Inject(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
        ordinal = 0
    ), remap = true)
    public void drawRenderDistanceSquare(final MinimapSession minimapSession, final GuiGraphics guiGraphics, final MinimapProcessor minimap, final Player player, final Entity renderEntity, final Vec3 renderPos, final double playerDimDiv, final double mapDimensionScale, final int bufferSize, final int viewW, final float sizeFix, final float partial, final int level, final boolean retryIfError, final boolean useWorldMap, final boolean lockedNorth, final int shape, final double ps, final double pc, final boolean cave, final boolean circle, final CustomVertexConsumers cvc, final CallbackInfo ci,
                                         @Local(name = "xFloored") int xFloored,
                                         @Local(name = "zFloored") int zFloored,
                                         @Local(name = "renderTypeBuffers") MultiBufferSource.BufferSource renderTypeBuffers,
                                         @Local(name = "matrixStack") PoseStack matrixStack
    ) {
        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != Minecraft.getInstance().level.dimension();
        if (!Settings.REGISTRY.showRenderDistanceSetting.get() || isDimensionSwitched) return;
        double actualPlayerX = minimap.getEntityRadar().getEntityX(mc.player, partial);
        double actualPlayerZ = minimap.getEntityRadar().getEntityZ(mc.player, partial);
        int actualXFloored = OptimizedMath.myFloor(actualPlayerX);
        int actualZFloored = OptimizedMath.myFloor(actualPlayerZ);
        final int viewDistance = mc.options.serverRenderDistance;
        int width = viewDistance * 2 + 1;
        // origin of the chunk we are standing in
        final int middleChunkX = -(actualXFloored & 15);
        final int middleChunkZ = -(actualZFloored & 15);
        int distanceFlooredX = actualXFloored - xFloored;
        int distanceFlooredZ = actualZFloored - zFloored;

        final int x0 = distanceFlooredX + middleChunkX - (width / 2) * 16;
        final int z0 = distanceFlooredZ + middleChunkZ - (width / 2) * 16;
        final int x1 = x0 + width * 16;
        final int z1 = z0 + width * 16;
        VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_LINES);
        MinimapShaders.FRAMEBUFFER_LINES.setFrameSize((float) scalingFramebuffer.viewWidth, (float) scalingFramebuffer.viewHeight);
        float lineWidth = Math.max(1.0f, modMain.getSettings().chunkGridLineWidth * Globals.minimapScaleMultiplier / (float) Globals.minimapSizeMultiplier);
        RenderSystem.lineWidth(lineWidth);
        float r = 1.0f;
        float g = 1.0f;
        float b = 0.0f;
        float a = 0.8f;
        PoseStack.Pose matrices = matrixStack.last();

        helper.addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x0, z0, x1, z0,
            r, g, b, a
        );
        helper.addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x1, z0, x1, z1,
            r, g, b, a
        );
        helper.addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x1, z1, x0, z1,
            r, g, b, a
        );
        helper.addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x0, z0, x0, z1,
            r, g, b, a
        );
    }

    @Inject(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
        ordinal = 0
    ), remap = true)
    public void drawWorldBorderSquare(final MinimapSession minimapSession, final GuiGraphics guiGraphics, final MinimapProcessor minimap, final Player player, final Entity renderEntity, final Vec3 renderPos, final double playerDimDiv, final double mapDimensionScale, final int bufferSize, final int viewW, final float sizeFix, final float partial, final int level, final boolean retryIfError, final boolean useWorldMap, final boolean lockedNorth, final int shape, final double ps, final double pc, final boolean cave, final boolean circle, final CustomVertexConsumers cvc, final CallbackInfo ci,
                                         @Local(name = "xFloored") int xFloored,
                                         @Local(name = "zFloored") int zFloored,
                                         @Local(name = "renderTypeBuffers") MultiBufferSource.BufferSource renderTypeBuffers,
                                         @Local(name = "matrixStack") PoseStack matrixStack
    ) {
        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != Minecraft.getInstance().level.dimension();
        if (!Settings.REGISTRY.showWorldBorderSetting.get() || isDimensionSwitched) return;
        var worldBorder = mc.level.getWorldBorder();
        float wbMinX = (float) worldBorder.getMinX();
        float wbMinZ = (float) worldBorder.getMinZ();
        float wbMaxX = (float) worldBorder.getMaxX();
        float wbMaxZ = (float) worldBorder.getMaxZ();
        float x0 = wbMinX - xFloored;
        float z0 = wbMinZ - zFloored;
        float x1 = wbMaxX - xFloored;
        float z1 = wbMaxZ - zFloored;

        VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_LINES);
        MinimapShaders.FRAMEBUFFER_LINES.setFrameSize((float) scalingFramebuffer.viewWidth, (float) scalingFramebuffer.viewHeight);
        float lineWidth = Math.max(1.0f, modMain.getSettings().chunkGridLineWidth * Globals.minimapScaleMultiplier / (float) Globals.minimapSizeMultiplier);
        RenderSystem.lineWidth(lineWidth);
        var matrix = matrixStack.last();
        float r = 0.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 0.8f;
        helper.addColoredLineToExistingBuffer(
            matrix, lineBufferBuilder,
            x0, z0, x1, z0,
            r, g, b, a
        );
        helper.addColoredLineToExistingBuffer(
            matrix, lineBufferBuilder,
            x0, z1, x1, z1,
            r, g, b, a
        );
        helper.addColoredLineToExistingBuffer(
            matrix, lineBufferBuilder,
            x1, z0, x1, z1,
            r, g, b, a
        );
        helper.addColoredLineToExistingBuffer(
            matrix, lineBufferBuilder,
            x0, z0, x0, z1,
            r, g, b, a
        );
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
    public void correctPostRotationTranslationForSizeMult(final MinimapSession minimapSession, final GuiGraphics guiGraphics, final MinimapProcessor minimap, final Player player, final Entity renderEntity, final Vec3 renderPos, final double playerDimDiv, final double mapDimensionScale, final int bufferSize, final int viewW, final float sizeFix, final float partial, final int level, final boolean retryIfError, final boolean useWorldMap, final boolean lockedNorth, final int shape, final double ps, final double pc, final boolean cave, final boolean circle, final CustomVertexConsumers cvc, final CallbackInfo ci,
                                                          @Local(name = "halfWView") float halfWView,
                                                          @Local(name = "shaderMatrixStack") Matrix4fStack shaderMatrixStack) {
        float sizeMultTranslation = (halfWView / Globals.minimapSizeMultiplier) * (Globals.minimapSizeMultiplier - 1);
        shaderMatrixStack.translate(sizeMultTranslation, sizeMultTranslation, 1.0f);
    }

    @Redirect(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapRendererHelper;drawMyTexturedModalRect(Lcom/mojang/blaze3d/vertex/PoseStack;FFIIFFFF)V"
    ), remap = true) // $REMAP
    public void redirectModelViewDraw(final MinimapRendererHelper instance, final PoseStack matrixStack, final float x, final float y, final int textureX, final int textureY, final float width, final float height, final float theight, final float factor,
                                      @Share("scaledSize") LocalIntRef scaledSize) {
        final float scaledSizeM = Globals.minimapScaleMultiplier * 512f;
        this.helper.drawMyTexturedModalRect(matrixStack, -scaledSize.get(), -scaledSize.get(), 0, 0, scaledSizeM, scaledSizeM, scaledSizeM, scaledSizeM);
    }

    @WrapOperation(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;draw(Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V"
    ))
    public void drawMinimapFeaturesCaveMode(final MultiTextureRenderTypeRendererProvider instance, final MultiTextureRenderTypeRenderer renderer, final Operation<Void> original,
                                            @Local(name = "xFloored") int xFloored,
                                            @Local(name = "zFloored") int zFloored,
                                            @Local(name = "overlayBufferBuilder") VertexConsumer overlayBufferBuilder,
                                            @Local(name = "matrixStack") PoseStack matrixStack,
                                            @Local(name = "minX") int minXRef,
                                            @Local(name = "maxX") int maxXRef,
                                            @Local(name = "minZ") int minZRef,
                                            @Local(name = "maxZ") int maxZRef
    ) {
        original.call(instance, renderer);
        int mapX = xFloored >> 4;
        int mapZ = zFloored >> 4;
        int chunkX = mapX >> 2;
        int chunkZ = mapZ >> 2;
        int tileX = mapX & 3;
        int tileZ = mapZ & 3;
        int insideX = xFloored & 15;
        int insideZ = zFloored & 15;
        Globals.drawManager.drawMinimapFeatures(
            minXRef, maxXRef, minZRef, maxZRef,
            chunkX, chunkZ,
            tileX, tileZ,
            insideX, insideZ,
            matrixStack, overlayBufferBuilder, helper
        );
    }
}
