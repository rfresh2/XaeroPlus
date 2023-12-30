package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.XaeroMinimapSession;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.region.MapTileChunk;
import xaeroplus.Globals;
import xaeroplus.feature.render.MinimapBackgroundDrawHelper;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class MixinSupportXaeroWorldmap {
    @Inject(method = "drawMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/settings/ModSettings;getRegionCacheHashCode()I"
    ), remap = true)
    public void overrideRegionRange(final XaeroMinimapSession minimapSession, final MatrixStack matrixStack, final MinimapRendererHelper helper, final int xFloored, final int zFloored, final int minViewX, final int minViewZ, final int maxViewX, final int maxViewZ, final boolean zooming, final double zoom, final double mapDimensionScale, final VertexConsumer overlayBufferBuilder, final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, final CallbackInfo ci,
                                  @Local(name = "mapX") int mapX,
                                  @Local(name = "mapZ") int mapZ,
                                  @Local(name = "minX") LocalIntRef minXRef,
                                  @Local(name = "maxX") LocalIntRef maxXRef,
                                  @Local(name = "minZ") LocalIntRef minZRef,
                                  @Local(name = "maxZ") LocalIntRef maxZRef
    ) {
        final int scaledSize = Globals.minimapScalingFactor * 4;
        minXRef.set((mapX >> 2) - scaledSize);
        maxXRef.set((mapX >> 2) + scaledSize);
        minZRef.set((mapZ >> 2) - scaledSize);
        maxZRef.set((mapZ >> 2) + scaledSize);
    }

    @Inject(method = "renderChunks", at = @At("HEAD"), remap = true)
    public void setupTransparentMMBgBuffer(final MatrixStack matrixStack, final int minX, final int maxX, final int minZ, final int maxZ, final int minViewX, final int maxViewX, final int minViewZ, final int maxViewZ, final MapProcessor mapProcessor, final int renderedCaveLayer, final boolean shouldRequestLoading, final boolean reloadEverything, final int globalReloadVersion, final int globalRegionCacheHashCode, final int globalCaveStart, final int globalCaveDepth, final boolean playerIsMoving, final boolean noCaveMaps, final boolean slimeChunks, final int chunkX, final int chunkZ, final int tileX, final int tileZ, final int insideX, final int insideZ, final Long seed, final MultiTextureRenderTypeRenderer mapWithLightRenderer, final MultiTextureRenderTypeRenderer mapNoLightRenderer, final MinimapRendererHelper helper, final VertexConsumer overlayBufferBuilder, final CallbackInfo ci,
                                           @Share("bgTesselator") LocalRef<Tessellator> bgTesselatorRef,
                                           @Share("bgBufferBuilder") LocalRef<BufferBuilder> bgBufferBuilderRef
    ) {
        if (XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) {
            var bgTesselator = Tessellator.getInstance();
            bgTesselatorRef.set(bgTesselator);
            var bgBufferBuilder = bgTesselator.getBuffer();
            bgBufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bgBufferBuilderRef.set(bgBufferBuilder);
        }
    }

    @Inject(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lorg/lwjgl/opengl/GL11;glTexParameterf(IIF)V"
    ), remap = true)
    public void renderTransparentMMBg(final MatrixStack matrixStack, final int minX, final int maxX, final int minZ, final int maxZ, final int minViewX, final int maxViewX, final int minViewZ, final int maxViewZ, final MapProcessor mapProcessor, final int renderedCaveLayer, final boolean shouldRequestLoading, final boolean reloadEverything, final int globalReloadVersion, final int globalRegionCacheHashCode, final int globalCaveStart, final int globalCaveDepth, final boolean playerIsMoving, final boolean noCaveMaps, final boolean slimeChunks, final int chunkX, final int chunkZ, final int tileX, final int tileZ, final int insideX, final int insideZ, final Long seed, final MultiTextureRenderTypeRenderer mapWithLightRenderer, final MultiTextureRenderTypeRenderer mapNoLightRenderer, final MinimapRendererHelper helper, final VertexConsumer overlayBufferBuilder, final CallbackInfo ci,
                                      @Share("bgBufferBuilder") LocalRef<BufferBuilder> bgBufferBuilderRef,
                                      @Local(name = "chunk") MapTileChunk chunk
    ) {
        if (XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) {
            // need these calc'd before they're init lol
            var drawX = ((chunk.getX() - chunkX) << 6) - (tileX << 4) - insideX;
            var drawZ = ((chunk.getZ() - chunkZ) << 6) - (tileZ << 4) - insideZ;
            MinimapBackgroundDrawHelper.addMMBackgroundToBuffer(Globals.minimapDrawContext.getMatrices().peek().getPositionMatrix(),
                                                                bgBufferBuilderRef.get(),
                                                                drawX,
                                                                drawZ,
                                                                chunk);
        }
    }

    @WrapWithCondition(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/mods/SupportXaeroWorldmap;renderSlimeChunks(Lxaero/map/region/MapTileChunk;Ljava/lang/Long;IILnet/minecraft/client/util/math/MatrixStack;Lxaero/common/minimap/render/MinimapRendererHelper;Lnet/minecraft/client/render/VertexConsumer;)V"
    ), remap = true)
    public boolean hideSlimeChunksWhileDimSwitched(SupportXaeroWorldmap instance, MapTileChunk chunk, Long seed, int drawX, int drawZ, MatrixStack matrixStack, MinimapRendererHelper helper, VertexConsumer overlayBufferBuilder) {
        return Globals.getCurrentDimensionId() != MinecraftClient.getInstance().world.getRegistryKey();
    }

    @Inject(method = "renderChunks", at = @At("TAIL"), remap = true)
    public void drawXPFeatures(final MatrixStack matrixStack, final int minX, final int maxX, final int minZ, final int maxZ, final int minViewX, final int maxViewX, final int minViewZ, final int maxViewZ, final MapProcessor mapProcessor, final int renderedCaveLayer, final boolean shouldRequestLoading, final boolean reloadEverything, final int globalReloadVersion, final int globalRegionCacheHashCode, final int globalCaveStart, final int globalCaveDepth, final boolean playerIsMoving, final boolean noCaveMaps, final boolean slimeChunks, final int chunkX, final int chunkZ, final int tileX, final int tileZ, final int insideX, final int insideZ, final Long seed, final MultiTextureRenderTypeRenderer mapWithLightRenderer, final MultiTextureRenderTypeRenderer mapNoLightRenderer, final MinimapRendererHelper helper, final VertexConsumer overlayBufferBuilder, final CallbackInfo ci,
                               @Share("bgTesselator") LocalRef<Tessellator> bgTesselatorRef) {
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
        if (XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) bgTesselatorRef.get().draw();
    }

    @Inject(method = "tryToGetMultiworldId", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/WorldMapSession;getMapProcessor()Lxaero/map/MapProcessor;"
    ), cancellable = true, remap = true)
    public void preventPossibleNPE(final RegistryKey<World> dimId, final CallbackInfoReturnable<String> cir,
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
