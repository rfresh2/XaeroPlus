package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.hud.minimap.module.MinimapSession;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.region.MapTileChunk;
import xaeroplus.Globals;
import xaeroplus.feature.render.MinimapBackgroundDrawHelper;
import xaeroplus.settings.Settings;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class MixinSupportXaeroWorldmap {
    @Inject(method = "drawMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/settings/ModSettings;getSlimeChunks(Lxaero/hud/minimap/module/MinimapSession;)Z"
    ), remap = false)
    public void overrideRegionRange(final MinimapSession minimapSession, final PoseStack matrixStack, final MinimapRendererHelper helper, final int xFloored, final int zFloored, final int minViewX, final int minViewZ, final int maxViewX, final int maxViewZ, final boolean zooming, final double zoom, final double mapDimensionScale, final VertexConsumer overlayBufferBuilder, final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, final CallbackInfo ci,
                                    @Local(name = "mapX") int mapX,
                                    @Local(name = "mapZ") int mapZ,
                                    @Local(name = "minX") LocalIntRef minXRef,
                                    @Local(name = "maxX") LocalIntRef maxXRef,
                                    @Local(name = "minZ") LocalIntRef minZRef,
                                    @Local(name = "maxZ") LocalIntRef maxZRef
    ) {
        final int scaledSize = Globals.minimapScaleMultiplier * 4;
        minXRef.set((mapX >> 2) - scaledSize);
        maxXRef.set((mapX >> 2) + scaledSize);
        minZRef.set((mapZ >> 2) - scaledSize);
        maxZRef.set((mapZ >> 2) + scaledSize);
    }

    @Inject(method = "renderChunks", at = @At("HEAD"), remap = false)
    public void setupTransparentMMBgBuffer(final PoseStack matrixStack, final int minX, final int maxX, final int minZ, final int maxZ, final int minViewX, final int maxViewX, final int minViewZ, final int maxViewZ, final MapProcessor mapProcessor, final boolean noCaveMaps, final boolean slimeChunks, final int chunkX, final int chunkZ, final int tileX, final int tileZ, final int insideX, final int insideZ, final Long seed, final MultiTextureRenderTypeRenderer mapWithLightRenderer, final MultiTextureRenderTypeRenderer mapNoLightRenderer, final MinimapRendererHelper helper, final VertexConsumer overlayBufferBuilder, final CallbackInfo ci,
                                           @Share("bgTesselator") LocalRef<Tesselator> bgTesselatorRef,
                                           @Share("bgBufferBuilder") LocalRef<BufferBuilder> bgBufferBuilderRef
    ) {
        if (Settings.REGISTRY.transparentMinimapBackground.get()) {
            var bgTesselator = Tesselator.getInstance();
            bgTesselatorRef.set(bgTesselator);
            var bgBufferBuilder = bgTesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bgBufferBuilderRef.set(bgBufferBuilder);
        }
    }

    @Inject(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/mods/SupportXaeroWorldmap;prepareMapTexturedRect(Lorg/joml/Matrix4f;FFIIFFLxaero/map/region/MapTileChunk;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;Lxaero/common/minimap/render/MinimapRendererHelper;)V"
    ), remap = false)
    public void buildTransparentMMBg(final PoseStack matrixStack, final int minX, final int maxX, final int minZ, final int maxZ, final int minViewX, final int maxViewX, final int minViewZ, final int maxViewZ, final MapProcessor mapProcessor, final boolean noCaveMaps, final boolean slimeChunks, final int chunkX, final int chunkZ, final int tileX, final int tileZ, final int insideX, final int insideZ, final Long seed, final MultiTextureRenderTypeRenderer mapWithLightRenderer, final MultiTextureRenderTypeRenderer mapNoLightRenderer, final MinimapRendererHelper helper, final VertexConsumer overlayBufferBuilder, final CallbackInfo ci,
                                      @Share("bgBufferBuilder") LocalRef<BufferBuilder> bgBufferBuilderRef,
                                      @Local(name = "drawX") int drawX,
                                      @Local(name = "drawZ") int drawZ,
                                      @Local(name = "chunk") MapTileChunk chunk
    ) {
        if (Settings.REGISTRY.transparentMinimapBackground.get()) {
            MinimapBackgroundDrawHelper.addMMBackgroundToBuffer(matrixStack.last().pose(),
                                                                bgBufferBuilderRef.get(),
                                                                drawX,
                                                                drawZ,
                                                                chunk);
        }
    }

    @WrapWithCondition(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/mods/SupportXaeroWorldmap;renderSlimeChunks(Lxaero/map/region/MapTileChunk;Ljava/lang/Long;IILcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
    ), remap = true) // $REMAP
    public boolean hideSlimeChunksWhileDimSwitched(SupportXaeroWorldmap instance, MapTileChunk chunk, Long seed, int drawX, int drawZ, PoseStack matrixStack, MinimapRendererHelper helper, VertexConsumer overlayBufferBuilder) {
        return Globals.getCurrentDimensionId() != Minecraft.getInstance().level.dimension();
    }

    @Inject(method = "renderChunks", at = @At("RETURN"), remap = false)
    public void drawTransparentMMBackground(final PoseStack matrixStack, final int minX, final int maxX, final int minZ, final int maxZ, final int minViewX, final int maxViewX, final int minViewZ, final int maxViewZ, final MapProcessor mapProcessor, final boolean noCaveMaps, final boolean slimeChunks, final int chunkX, final int chunkZ, final int tileX, final int tileZ, final int insideX, final int insideZ, final Long seed, final MultiTextureRenderTypeRenderer mapWithLightRenderer, final MultiTextureRenderTypeRenderer mapNoLightRenderer, final MinimapRendererHelper helper, final VertexConsumer overlayBufferBuilder, final CallbackInfo ci,
                               @Share("bgBufferBuilder") LocalRef<BufferBuilder> bgBufferBuilderRef) {
        if (Settings.REGISTRY.transparentMinimapBackground.get()) {
            MeshData meshData = bgBufferBuilderRef.get().build();
            if (meshData != null) BufferUploader.drawWithShader(meshData);
        }
    }

    @Inject(method = "drawMinimap", at = @At(value = "RETURN"), remap = false)
    public void drawXPShaderFeatures(final MinimapSession minimapSession, final PoseStack matrixStack, final MinimapRendererHelper helper, final int xFloored, final int zFloored, final int minViewX, final int minViewZ, final int maxViewX, final int maxViewZ, final boolean zooming, final double zoom, final double mapDimensionScale, final VertexConsumer overlayBufferBuilder, final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, final CallbackInfo ci) {
        int mapX = xFloored >> 4;
        int mapZ = zFloored >> 4;
        int chunkX = mapX >> 2;
        int chunkZ = mapZ >> 2;
        int tileX = mapX & 3;
        int tileZ = mapZ & 3;
        int insideX = xFloored & 15;
        int insideZ = zFloored & 15;
        Globals.drawManager.drawMinimapFeatures(
            minViewX,
            maxViewX,
            minViewZ,
            maxViewZ,
            chunkX,
            chunkZ,
            tileX,
            tileZ,
            insideX,
            insideZ,
            matrixStack,
            overlayBufferBuilder,
            helper);
    }

    @Inject(method = "tryToGetMultiworldId", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/WorldMapSession;getMapProcessor()Lxaero/map/MapProcessor;"
    ), cancellable = true, remap = false)
    public void preventPossibleNPE(final ResourceKey<Level> dimId, final CallbackInfoReturnable<String> cir,
                                   @Local WorldMapSession session) {
        // possible race condition where WM session is not initialized when client ticks start
        /**
         * Caused by: java.lang.NullPointerException: Cannot invoke "xaero.map.WorldMapSession.getMapProcessor()" because "worldmapSession" is null
         *     at xaero.common.mods.SupportXaeroWorldmap.tryToGetMultiworldId(SupportXaeroWorldmap.java:362)
         *     at xaero.common.minimap.waypoints.WaypointsManager.getNewAutoWorldID(WaypointsManager.java:194)
         *     at xaero.common.minimap.waypoints.WaypointsManager.updateWorldIds(WaypointsManager.java:381)
         *     at xaero.common.events.FMLEventHandler.handlePlayerTickStart(FMLEventHandler.java:40)
         *     at xaero.common.events.FMLCommonEventHandler.handlePlayerTickStart(FMLCommonEventHandler.java:21)
         *     at net.minecraft.class_1657.handler$dae000$xaerominimap$onTickStart(class_1657.java:6382)
         */
        if (session == null) {
            cir.setReturnValue(null);
        }
    }
}
