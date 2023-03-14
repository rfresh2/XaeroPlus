package xaeroplus.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.region.MinimapTile;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.NewChunks;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.WDLHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class MixinSupportXaeroWorldmap {
    @Shadow
    public int compatibilityVersion;
    @Shadow
    private IXaeroMinimap modMain;
    @Final
    @Shadow
    private static HashMap<MapTileChunk, Long> seedsUsed;
    @Shadow
    public abstract float getMinimapBrightness();
    @Shadow
    protected abstract void bindMapTextureWithLighting(int compatibilityVersion, float brightness, MapTileChunk chunk, boolean zooming);

    /**
     * @author rfresh2
     * @reason Render NewChunks on minimap
     */
    @Overwrite
    public void drawMinimap(XaeroMinimapSession minimapSession, MinimapRendererHelper helper, int xFloored, int zFloored, int minViewX, int minViewZ, int maxViewX, int maxViewZ, boolean zooming, double zoom) {
        WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
        if (worldmapSession != null) {
            MapProcessor mapProcessor = worldmapSession.getMapProcessor();
            synchronized(mapProcessor.renderThreadPauseSync) {
                if (!mapProcessor.isRenderingPaused()) {
                    if (mapProcessor.getCurrentDimension() == null) {
                        return;
                    }

                    int compatibilityVersion = this.compatibilityVersion;
                    String worldString = compatibilityVersion >= 7 ? mapProcessor.getCurrentWorldId() : mapProcessor.getCurrentWorldString();
                    if (worldString == null) {
                        return;
                    }

                    int mapX = xFloored >> 4;
                    int mapZ = zFloored >> 4;
                    int chunkX = mapX >> 2;
                    int chunkZ = mapZ >> 2;
                    int tileX = mapX & 3;
                    int tileZ = mapZ & 3;
                    int insideX = xFloored & 15;
                    int insideZ = zFloored & 15;
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    GlStateManager.enableBlend();
                    int minX = (mapX >> 2) - 4;
                    int maxX = (mapX >> 2) + 4;
                    int minZ = (mapZ >> 2) - 4;
                    int maxZ = (mapZ >> 2) + 4;
                    float brightness = this.getMinimapBrightness();
                    boolean wmUsesHashcodes = compatibilityVersion >= 5;
                    int globalRegionCacheHashCode = wmUsesHashcodes ? WorldMap.settings.getRegionCacheHashCode() : 0;
                    boolean reloadEverything = wmUsesHashcodes ? WorldMap.settings.reloadEverything : false;
                    int globalReloadVersion = wmUsesHashcodes ? WorldMap.settings.reloadVersion : 0;
                    boolean slimeChunks = this.modMain.getSettings().getSlimeChunks(minimapSession.getWaypointsManager());
                    if (compatibilityVersion >= 7) {
                        GL14.glBlendFuncSeparate(1, 0, 0, 1);
                    }

                    if (compatibilityVersion >= 6) {
                        GuiMap.setupTextureMatricesAndTextures(brightness);
                    }

                    WaypointWorld world = minimapSession.getWaypointsManager().getAutoWorld();
                    Long seed = slimeChunks && world != null ? this.modMain.getSettings().getSlimeChunksSeed(world.getFullId()) : null;

                    for(int i = minX; i <= maxX; ++i) {
                        for(int j = minZ; j <= maxZ; ++j) {
                            MapRegion region = mapProcessor.getMapRegion(i >> 3, j >> 3, mapProcessor.regionExists(i >> 3, j >> 3));
                            if (region != null) {
                                int drawX;
                                int drawZ;
                                synchronized(region) {
                                    drawX = wmUsesHashcodes ? region.getCacheHashCode() : 0;
                                    drawZ = wmUsesHashcodes ? region.getReloadVersion() : 0;
                                    if (!region.recacheHasBeenRequested() && !region.reloadHasBeenRequested() && (region.getLoadState() == 0 || (region.getLoadState() == 4 || region.getLoadState() == 2 && region.isBeingWritten()) && (reloadEverything && drawZ != globalReloadVersion || drawX != globalRegionCacheHashCode || region.getVersion() != mapProcessor.getGlobalVersion() || compatibilityVersion >= 11 && (region.isMetaLoaded() || region.getLoadState() != 0 || !region.hasHadTerrain()) && region.getHighlightsHash() != region.getDim().getHighlightHandler().getRegionHash(region.getRegionX(), region.getRegionZ()) || region.getLoadState() != 2 && region.shouldCache()))) {
                                        if (region.getLoadState() == 2) {
                                            region.requestRefresh(mapProcessor);
                                        } else {
                                            mapProcessor.getMapSaveLoad().requestLoad(region, "Minimap", false);
                                            mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                                        }
                                    }
                                }

                                if (!mapProcessor.isUploadingPaused()) {
                                    if (compatibilityVersion >= 7) {
                                        if (region.isLoaded()) {
                                            mapProcessor.getMapWorld().getCurrentDimension().getMapRegions().bumpLoadedRegion(region);
                                        }
                                    } else {
                                        List<MapRegion> regions = mapProcessor.getMapWorld().getCurrentDimension().getMapRegionsList();
                                        regions.remove(region);
                                        regions.add(region);
                                    }
                                }

                                if (i >= minViewX && i <= maxViewX && j >= minViewZ && j <= maxViewZ) {
                                    MapTileChunk chunk = region.getChunk(i & 7, j & 7);
                                    if (chunk != null && chunk.getGlColorTexture() != -1) {
                                        this.bindMapTextureWithLighting(compatibilityVersion, brightness, chunk, zooming);
                                        if (zooming && compatibilityVersion >= 12) {
                                            GlStateManager.setActiveTexture(33984);
                                            GL11.glTexParameteri(3553, 10240, 9729);
                                        }

                                        GL11.glTexParameterf(3553, 33082, 0.0F);
                                        drawX = 64 * (chunk.getX() - chunkX) - 16 * tileX - insideX;
                                        drawZ = 64 * (chunk.getZ() - chunkZ) - 16 * tileZ - insideZ;
                                        if (compatibilityVersion < 7) {
                                            GL14.glBlendFuncSeparate(770, 771, 1, 771);
                                            GuiMap.renderTexturedModalRectWithLighting((float)drawX, (float)drawZ, 0, 0, 64.0F, 64.0F);
                                        } else {
                                            GuiMap.renderTexturedModalRectWithLighting((float)drawX, (float)drawZ, 64.0F, 64.0F);
                                        }

                                        if (zooming && compatibilityVersion >= 12) {
                                            GlStateManager.setActiveTexture(33984);
                                            GL11.glTexParameteri(3553, 10240, 9728);
                                        }

                                        if (slimeChunks) {
                                            GuiMap.restoreTextureStates();
                                            if (compatibilityVersion >= 7) {
                                                GL14.glBlendFuncSeparate(770, 771, 1, 771);
                                            }

                                            Long savedSeed = (Long)seedsUsed.get(chunk);
                                            boolean newSeed = seed == null && savedSeed != null || seed != null && !seed.equals(savedSeed);
                                            if (newSeed) {
                                                seedsUsed.put(chunk, seed);
                                            }

                                            for(int t = 0; t < 16; ++t) {
                                                if (newSeed || (chunk.getTileGridsCache()[t % 4][t / 4] & 1) == 0) {
                                                    chunk.getTileGridsCache()[t % 4][t / 4] = (byte)(1 | (MinimapTile.isSlimeChunk(this.modMain.getSettings(), chunk.getX() * 4 + t % 4, chunk.getZ() * 4 + t / 4, seed) ? 2 : 0));
                                                }

                                                if ((chunk.getTileGridsCache()[t % 4][t / 4] & 2) != 0) {
                                                    int slimeDrawX = drawX + 16 * (t % 4);
                                                    int slimeDrawZ = drawZ + 16 * (t / 4);
                                                    Gui.drawRect(slimeDrawX, slimeDrawZ, slimeDrawX + 16, slimeDrawZ + 16, -2142047936);
                                                }
                                            }

                                            if (compatibilityVersion >= 6) {
                                                GuiMap.setupTextures(brightness);
                                            }

                                            if (compatibilityVersion >= 7) {
                                                GL14.glBlendFuncSeparate(1, 0, 0, 1);
                                                GlStateManager.enableBlend();
                                            }
                                        }

                                        if (XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue()) {
                                            GuiMap.restoreTextureStates();
                                            if (compatibilityVersion >= 7) {
                                                GL14.glBlendFuncSeparate(770, 771, 1, 771);
                                            }
                                            for(int t = 0; t < 16; ++t) {
                                                final int chunkPosX = chunk.getX() * 4 + t % 4;
                                                final int chunkPosZ = chunk.getZ() * 4 + t / 4;
                                                if (ModuleManager.getModule(NewChunks.class).isNewChunk(chunkPosX, chunkPosZ)) {
                                                    int newChunkDrawX = drawX + 16 * (t % 4);
                                                    int newChunkDrawZ = drawZ + 16 * (t / 4);
                                                    Gui.drawRect(newChunkDrawX, newChunkDrawZ, newChunkDrawX + 16, newChunkDrawZ + 16, ModuleManager.getModule(NewChunks.class).getNewChunksColor());
                                                }
                                            }

                                            if (compatibilityVersion >= 6) {
                                                GuiMap.setupTextures(brightness);
                                            }

                                            if (compatibilityVersion >= 7) {
                                                GL14.glBlendFuncSeparate(1, 0, 0, 1);
                                                GlStateManager.enableBlend();
                                            }
                                        }

                                        if (XaeroPlusSettingRegistry.wdlEnabledSetting.getValue()
                                                && WDLHelper.isWdlPresent()
                                                && WDLHelper.isDownloading()) {
                                            GuiMap.restoreTextureStates();
                                            final Set<Long> wdlSavedChunksWithCache = WDLHelper.getSavedChunksWithCache();
                                            if (compatibilityVersion >= 7) {
                                                GL14.glBlendFuncSeparate(770, 771, 1, 771);
                                            }
                                            for(int t = 0; t < 16; ++t) {
                                                final Long chunkPos = ChunkUtils.chunkPosToLong(chunk.getX() * 4 + t % 4, chunk.getZ() * 4 + t / 4);
                                                if (wdlSavedChunksWithCache.contains(chunkPos)) {
                                                    int wdlChunkDrawX = drawX + 16 * (t % 4);
                                                    int wdlChunkDrawZ = drawZ + 16 * (t / 4);
                                                    Gui.drawRect(wdlChunkDrawX, wdlChunkDrawZ, wdlChunkDrawX + 16, wdlChunkDrawZ + 16, WDLHelper.getWdlColor());
                                                }
                                            }

                                            if (compatibilityVersion >= 6) {
                                                GuiMap.setupTextures(brightness);
                                            }

                                            if (compatibilityVersion >= 7) {
                                                GL14.glBlendFuncSeparate(1, 0, 0, 1);
                                                GlStateManager.enableBlend();
                                            }
                                        }

                                        if (compatibilityVersion < 7) {
                                            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                                            GlStateManager.enableBlend();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TODO: moving this code to MinimapFBORenderer would allow it to render on top of grid lines
                    if (XaeroPlusSettingRegistry.showRenderDistanceSetting.getValue()) {
                        GuiMap.restoreTextureStates();
                        if (compatibilityVersion >= 7) {
                            GL14.glBlendFuncSeparate(770, 771, 1, 771);
                        }

                        final int setting = (int) XaeroPlusSettingRegistry.assumedServerRenderDistanceSetting.getValue();
                        final int width = setting * 2 + 1;
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
                        GlStateManager.glLineWidth((float)this.modMain.getSettings().chunkGridLineWidth);
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
                    GL14.glBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableBlend();
                }

            }
        }
    }


}
