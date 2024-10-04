package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.hud.minimap.module.MinimapSession;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.region.MapTileChunk;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.Globals;
import xaeroplus.util.GuiHelper;

import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static xaeroplus.settings.XaeroPlusSettingRegistry.transparentMinimapBackground;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class MixinSupportXaeroWorldmap {
    @Shadow
    public int compatibilityVersion;
    @Shadow
    private IXaeroMinimap modMain;
    @Shadow
    public abstract float getMinimapBrightness();

    @Inject(method = "drawMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/settings/ModSettings;getRegionCacheHashCode()I"
    ), remap = false)
    public void overrideRegionRange(MinimapSession minimapSession, MinimapRendererHelper helper, int xFloored, int zFloored, int minViewX, int minViewZ, int maxViewX, int maxViewZ, boolean zooming, double zoom, double mapDimensionScale, final CallbackInfo ci,
                                    @Local(name = "mapX") int mapX,
                                    @Local(name = "mapZ") int mapZ,
                                    @Local(name = "minX") LocalIntRef minXRef,
                                    @Local(name = "maxX") LocalIntRef maxXRef,
                                    @Local(name = "minZ") LocalIntRef minZRef,
                                    @Local(name = "maxZ") LocalIntRef maxZRef) {
        final int scaledSize = Globals.minimapScalingFactor * 4;
        minXRef.set((mapX >> 2) - scaledSize);
        maxXRef.set((mapX >> 2) + scaledSize);
        minZRef.set((mapZ >> 2) - scaledSize);
        maxZRef.set((mapZ >> 2) + scaledSize);
    }

    @Inject(method = "renderChunks", at = @At("RETURN"))
    public void drawRenderDistance(final int minX, final int maxX, final int minZ, final int maxZ, final int minViewX, final int maxViewX, final int minViewZ, final int maxViewZ, final MapProcessor mapProcessor, final int renderedCaveLayer, final boolean shouldRequestLoading, final boolean reloadEverything, final int globalReloadVersion, final int globalRegionCacheHashCode, final int globalCaveStart, final int globalCaveDepth, final boolean playerIsMoving, final boolean noCaveMaps, final boolean slimeChunks, final int chunkX, final int chunkZ, final int tileX, final int tileZ, final int insideX, final int insideZ, final Long seed, final boolean wmHasCaveLayers, final boolean wmUsesHashcodes, final float brightness, final boolean zooming, final MinimapRendererHelper helper, final CallbackInfo ci) {
        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != Minecraft.getMinecraft().player.dimension;
        if (XaeroPlusSettingRegistry.showRenderDistanceSetting.getValue() && !isDimensionSwitched) {
            GuiMap.restoreTextureStates();
            if (compatibilityVersion >= 7) {
                GL14.glBlendFuncSeparate(770, 771, 1, 771);
            }

            final int setting = (int) XaeroPlusSettingRegistry.assumedServerRenderDistanceSetting.getValue();
            int width = setting * 2 + 1;
            // origin of the chunk we are standing in
            final int middleChunkX = -insideX;
            final int middleChunkZ = -insideZ;
            // this is biased to +x/+z for even sizes which im not certain is correct
            final int x0 = middleChunkX - (width / 2) * 16;
            final int z0 = middleChunkZ - (width / 2) * 16;
            final int x1 = x0 + width * 16;
            final int z1 = z0 + width * 16;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertexBuffer = tessellator.getBuffer();
            vertexBuffer.begin(GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            // yellow
            GlStateManager.color(1.f, 1.f, 0.f, 0.8F);
            GlStateManager.glLineWidth((float) this.modMain.getSettings().chunkGridLineWidth * Globals.minimapScalingFactor);
            vertexBuffer.pos(x0, z0, 0.0).endVertex();
            vertexBuffer.pos(x1, z0, 0.0).endVertex();
            vertexBuffer.pos(x1, z1, 0.0).endVertex();
            vertexBuffer.pos(x0, z1, 0.0).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();

            if (compatibilityVersion >= 6) {
                GuiMap.setupTextures(brightness);
            }

            if (compatibilityVersion >= 7) {
                GL14.glBlendFuncSeparate(1, 0, 0, 1);
                GlStateManager.enableBlend();
            }
        }
        GuiMap.restoreTextureStates();
    }

    @Inject(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/mods/SupportXaeroWorldmap;bindMapTextureWithLighting(IFLxaero/map/region/MapTileChunk;Z)V"
    ))
    public void drawTransparentMMBackground(final int minX, final int maxX, final int minZ, final int maxZ, final int minViewX, final int maxViewX, final int minViewZ, final int maxViewZ, final MapProcessor mapProcessor, final int renderedCaveLayer, final boolean shouldRequestLoading, final boolean reloadEverything, final int globalReloadVersion, final int globalRegionCacheHashCode, final int globalCaveStart, final int globalCaveDepth, final boolean playerIsMoving, final boolean noCaveMaps, final boolean slimeChunks, final int chunkX, final int chunkZ, final int tileX, final int tileZ, final int insideX, final int insideZ, final Long seed, final boolean wmHasCaveLayers, final boolean wmUsesHashcodes, final float brightness, final boolean zooming, final MinimapRendererHelper helper, final CallbackInfo ci,
                                            @Local(name = "chunk") MapTileChunk chunk) {
        int drawX = ((chunk.getX() - chunkX) << 6) - (tileX << 4) - insideX;
        int drawZ = ((chunk.getZ() - chunkZ) << 6) - (tileZ << 4) - insideZ;
        if (transparentMinimapBackground.getValue()) {
            GuiHelper.drawMMBackground(drawX, drawZ, brightness, chunk);
            GuiMap.setupTextureMatricesAndTextures(brightness);
        }
    }

    @WrapOperation(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/GuiMap;renderTexturedModalRectWithLighting(FFIIFF)V"
    ))
    public void finishTransparentMMBackgroundA(final float x, final float y, final int textureX, final int textureY, final float width, final float height, final Operation<Void> original) {
        original.call(x, y, textureX, textureY, width, height);
        if (transparentMinimapBackground.getValue()) {
            GuiHelper.finishMMSetup(compatibilityVersion, getMinimapBrightness(), null, false);
        }
    }

    @WrapOperation(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/GuiMap;renderTexturedModalRectWithLighting(FFFF)V"
    ))
    public void finishTransparentMMBackgroundB(final float x, final float y, final float width, final float height, final Operation<Void> original) {
        original.call(x, y, width, height);
        if (transparentMinimapBackground.getValue()) {
            GuiHelper.finishMMSetup(compatibilityVersion, getMinimapBrightness(), null, false);
        }
    }

//    @WrapOperation(method = "renderChunks", at = @At(
//        value = "FIELD",
//        target = "Lxaero/common/mods/SupportXaeroWorldmap;compatibilityVersion:I",
//        opcode = Opcodes.GETFIELD,
//        ordinal = 0
//    ),
//        slice = @Slice(
//            from = @At(
//                value = "INVOKE",
//                target = "Lxaero/common/mods/SupportXaeroWorldmap;renderSlimeChunks(Lxaero/map/region/MapTileChunk;Ljava/lang/Long;IIFLxaero/common/minimap/render/MinimapRendererHelper;)V"
//            )
//        ))
//    public int drawChunkHighlightFeatures(final SupportXaeroWorldmap instance, final Operation<Integer> original,
//                                          @Local(name = "drawX") int drawX,
//                                          @Local(name = "drawZ") int drawZ,
//                                          @Local(name = "chunk") MapTileChunk chunk,
//                                          @Local(name = "brightness") float brightness) {
//        GuiMap.restoreTextureStates();
//        if (compatibilityVersion >= 7) {
//            GL14.glBlendFuncSeparate(770, 771, 1, 771);
//        }
//        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != Minecraft.getMinecraft().player.dimension;
//        if (XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue()) {
//            final NewChunks newChunks = ModuleManager.getModule(NewChunks.class);
//            GuiHelper.drawMMHighlights(
//                (x, z) -> newChunks.isNewChunk(x, z, Globals.getCurrentDimensionId()),
//                drawX,
//                drawZ,
//                chunk.getX() << 2,
//                chunk.getZ() << 2,
//                newChunks.getNewChunksColor());
//        }
//        if (XaeroPlusSettingRegistry.portalSkipDetectionEnabledSetting.getValue() && XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue()) {
//            final PortalSkipDetection portalSkipDetection = ModuleManager.getModule(
//                PortalSkipDetection.class);
//            GuiHelper.drawMMHighlights(
//                portalSkipDetection::isPortalSkipChunk,
//                drawX,
//                drawZ,
//                chunk.getX() << 2,
//                chunk.getZ() << 2,
//                portalSkipDetection.getPortalSkipChunksColor());
//        }
//        if (XaeroPlusSettingRegistry.portalsEnabledSetting.getValue()) {
//            final Portals portalModule = ModuleManager.getModule(Portals.class);
//            GuiHelper.drawMMHighlights(
//                (x, z) -> portalModule.isPortalChunk(x, z, Globals.getCurrentDimensionId()),
//                drawX,
//                drawZ,
//                chunk.getX() << 2,
//                chunk.getZ() << 2,
//                portalModule.getPortalsColor());
//        }
//        if (XaeroPlusSettingRegistry.wdlEnabledSetting.getValue()
//            && WDLHelper.isWdlPresent()
//            && WDLHelper.isDownloading()
//            && !isDimensionSwitched) {
//            final Set<Long> wdlSavedChunksWithCache = WDLHelper.getSavedChunksWithCache();
//            GuiHelper.drawMMHighlights(
//                (x, z) -> wdlSavedChunksWithCache.contains(ChunkUtils.chunkPosToLong(x, z)),
//                drawX,
//                drawZ,
//                chunk.getX() << 2,
//                chunk.getZ() << 2,
//                WDLHelper.getWdlColor()
//            );
//        }
//        if (compatibilityVersion >= 6) {
//            GuiMap.setupTextures(brightness);
//        }
//
//        if (compatibilityVersion >= 7) {
//            GL14.glBlendFuncSeparate(1, 0, 0, 1);
//            GlStateManager.enableBlend();
//        }
//        return original.call(instance);
//    }
}
