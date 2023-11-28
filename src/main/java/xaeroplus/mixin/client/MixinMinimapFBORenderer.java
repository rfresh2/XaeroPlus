package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.IXaeroMinimap;
import xaero.common.MinimapLogs;
import xaero.common.XaeroMinimapSession;
import xaero.common.effect.Effects;
import xaero.common.graphics.ImprovedFramebuffer;
import xaero.common.minimap.MinimapInterface;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.element.render.map.MinimapElementMapRendererHandler;
import xaero.common.minimap.region.MinimapChunk;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.radar.EntityIconManager;
import xaero.common.minimap.render.radar.EntityIconPrerenderer;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaero.common.minimap.waypoints.render.CompassRenderer;
import xaero.common.minimap.waypoints.render.WaypointsGuiRenderer;
import xaero.common.misc.Misc;
import xaero.common.misc.OptimizedMath;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.CustomMinimapFBORenderer;
import xaeroplus.util.Globals;

@Mixin(value = MinimapFBORenderer.class, remap = false)
public abstract class MixinMinimapFBORenderer extends MinimapRenderer implements CustomMinimapFBORenderer {

    @Shadow
    private ImprovedFramebuffer scalingFramebuffer;
    @Shadow
    private ImprovedFramebuffer rotationFramebuffer;
    @Shadow
    private MinimapElementMapRendererHandler minimapElementMapRendererHandler;
    @Shadow
    private RadarRenderer radarRenderer;
    @Shadow
    private EntityIconManager entityIconManager;
    @Shadow
    private boolean triedFBO;
    @Shadow
    private boolean loadedFBO;

    public MixinMinimapFBORenderer(IXaeroMinimap modMain, Minecraft mc, WaypointsGuiRenderer waypointsGuiRenderer, MinimapInterface minimapInterface, CompassRenderer compassRenderer) {
        super(modMain, mc, waypointsGuiRenderer, minimapInterface, compassRenderer);
    }

    /**
     * @author rfresh2
     * @reason big minimap
     */
    @Overwrite
    public void loadFrameBuffer(MinimapProcessor minimapProcessor) {
        if (!minimapProcessor.canUseFrameBuffer()) {
            MinimapLogs.LOGGER.info("FBO mode not supported! Using minimap safe mode.");
        } else {
            reloadMapFrameBuffers();
            this.entityIconManager = new EntityIconManager(this.modMain, new EntityIconPrerenderer(this.modMain));
            this.minimapElementMapRendererHandler = MinimapElementMapRendererHandler.Builder.begin().build();
            this.radarRenderer = RadarRenderer.Builder.begin()
                    .setModMain(this.modMain)
                    .setEntityIconManager(this.entityIconManager)
                    .setMinimapInterface(this.minimapInterface)
                    .build();
            this.minimapElementMapRendererHandler.add(this.radarRenderer);
            this.minimapInterface.getOverMapRendererHandler().add(this.radarRenderer);
            if (this.modMain.getSupportMods().worldmap()) {
                this.modMain.getSupportMods().worldmapSupport.createRadarRenderWrapper(this.radarRenderer);
            }
        }

        this.triedFBO = true;
    }

    @Override
    public void reloadMapFrameBuffers() {
        if (!XaeroMinimapSession.getCurrentSession().getMinimapProcessor().canUseFrameBuffer()) {
            MinimapLogs.LOGGER.info("FBO mode not supported! Using minimap safe mode.");
        } else {
            if (this.scalingFramebuffer != null)
                this.scalingFramebuffer.deleteFramebuffer();
            if (this.rotationFramebuffer != null)
                this.rotationFramebuffer.deleteFramebuffer();
            // double the framebuffer size
            final int scaledSize = Globals.minimapScalingFactor * 512;
            this.scalingFramebuffer = new ImprovedFramebuffer(scaledSize, scaledSize, false);
            this.rotationFramebuffer = new ImprovedFramebuffer(scaledSize, scaledSize, false);
            this.rotationFramebuffer.setFramebufferFilter(9729);
            this.loadedFBO = this.scalingFramebuffer.framebufferObject != -1 && this.rotationFramebuffer.framebufferObject != -1;
        }
    }

    /**
     * @author rfresh2
     * @reason big minimap
     */
    @Overwrite
    public void renderChunksToFBO(
            XaeroMinimapSession minimapSession,
            MinimapProcessor minimap,
            EntityPlayer player,
            Entity renderEntity,
            double playerX,
            double playerZ,
            double playerDimDiv,
            double mapDimensionScale,
            int bufferSize,
            int viewW,
            float sizeFix,
            float partial,
            int level,
            boolean retryIfError,
            boolean useWorldMap,
            boolean lockedNorth,
            int shape,
            double ps,
            double pc,
            boolean cave,
            boolean circle,
            ScaledResolution scaledRes
    ) {
        viewW *= Globals.minimapScalingFactor;
        final int scaledSize = 256 * Globals.minimapScalingFactor;
        double maxVisibleLength = !lockedNorth && shape != 1 ? (double)viewW * Math.sqrt(2.0) : (double)viewW;
        double halfMaxVisibleLength = maxVisibleLength / 2.0;
        double radiusBlocks = maxVisibleLength / 2.0 /  this.zoom;
        int xFloored = OptimizedMath.myFloor(playerX);
        int zFloored = OptimizedMath.myFloor(playerZ);
        int playerChunkX = xFloored >> 6;
        int playerChunkZ = zFloored >> 6;
        int offsetX = xFloored & 63;
        int offsetZ = zFloored & 63;
        boolean zooming = (double)((int)this.zoom) != this.zoom;
        this.scalingFramebuffer.bindFramebuffer(true);
        GL11.glClear(16640);
        GlStateManager.enableTexture2D();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.clear(256);
        GlStateManager.matrixMode(5889);
        this.helper.defaultOrtho(this.scalingFramebuffer, scaledRes);
        GlStateManager.matrixMode(5888);
        GL11.glPushMatrix();
        GlStateManager.loadIdentity();
        double xInsidePixel = playerX - (double)xFloored;
        if (xInsidePixel < 0.0) {
            ++xInsidePixel;
        }

        double zInsidePixel = playerZ - (double)zFloored;
        if (zInsidePixel < 0.0) {
            ++zInsidePixel;
        }

        float halfWView = (float)viewW / 2.0F;
        float angle = (float)(90.0 - this.getRenderAngle(lockedNorth));
        GlStateManager.enableBlend();
        // update translation to 1024 buffer size
        GlStateManager.translate(scaledSize, scaledSize, -2000.0F);
        GlStateManager.scale(this.zoom, this.zoom, 1.0);
        if (!XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) {
            Gui.drawRect(-scaledSize, -scaledSize, scaledSize, scaledSize, ColorHelper.getColor(0, 0, 0, 255));
        } else {
            Gui.drawRect(-scaledSize, -scaledSize, scaledSize, scaledSize, ColorHelper.getColor(0, 0, 0, 0));
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        float chunkGridAlphaMultiplier = 1.0F;
        int minX = playerChunkX + (int)Math.floor(((double)offsetX - radiusBlocks) / 64.0);
        int minZ = playerChunkZ + (int)Math.floor(((double)offsetZ - radiusBlocks) / 64.0);
        int maxX = playerChunkX + (int)Math.floor(((double)(offsetX + 1) + radiusBlocks) / 64.0);
        int maxZ = playerChunkZ + (int)Math.floor(((double)(offsetZ + 1) + radiusBlocks) / 64.0);

        if (!cave || !this.mc.player.isPotionActive(Effects.NO_CAVE_MAPS) && !this.mc.player.isPotionActive(Effects.NO_CAVE_MAPS_HARMFUL)) {
            if (useWorldMap) {
                chunkGridAlphaMultiplier = this.modMain.getSupportMods().worldmapSupport.getMinimapBrightness();
                this.modMain
                        .getSupportMods()
                        .worldmapSupport
                        .drawMinimap(minimapSession, this.helper, xFloored, zFloored, minX, minZ, maxX, maxZ, zooming, this.zoom, mapDimensionScale);
            } else if (minimap.getMinimapWriter().getLoadedBlocks() != null && level >= 0) {
                int loadedLevels = minimap.getMinimapWriter().getLoadedLevels();
                chunkGridAlphaMultiplier = loadedLevels <= 1 ? 1.0F : 0.375F + 0.625F * (1.0F - (float)level / (float)(loadedLevels - 1));
                int loadedMapChunkX = minimap.getMinimapWriter().getLoadedMapChunkX();
                int loadedMapChunkZ = minimap.getMinimapWriter().getLoadedMapChunkZ();
                int loadedWidth = minimap.getMinimapWriter().getLoadedBlocks().length;
                boolean slimeChunks = this.modMain.getSettings().getSlimeChunks(minimapSession.getWaypointsManager());
                minX = Math.max(minX, loadedMapChunkX);
                minZ = Math.max(minZ, loadedMapChunkZ);
                maxX = Math.min(maxX, loadedMapChunkX + loadedWidth - 1);
                maxZ = Math.min(maxZ, loadedMapChunkZ + loadedWidth - 1);

                for(int X = minX; X <= maxX; ++X) {
                    int canvasX = X - minimap.getMinimapWriter().getLoadedMapChunkX();

                    for(int Z = minZ; Z <= maxZ; ++Z) {
                        int canvasZ = Z - minimap.getMinimapWriter().getLoadedMapChunkZ();
                        MinimapChunk mchunk = minimap.getMinimapWriter().getLoadedBlocks()[canvasX][canvasZ];
                        if (mchunk != null) {
                            mchunk.bindTexture(level);
                            if (mchunk.isHasSomething() && level < mchunk.getLevelsBuffered() && mchunk.getGlTexture(level) != 0) {
                                if (!zooming) {
                                    GL11.glTexParameteri(3553, 10240, 9728);
                                } else {
                                    GL11.glTexParameteri(3553, 10240, 9729);
                                }

                                int drawX = (X - playerChunkX) * 64 - offsetX;
                                int drawZ = (Z - playerChunkZ) * 64 - offsetZ;
                                GlStateManager.enableBlend();
                                GL14.glBlendFuncSeparate(770, 771, 1, 771);
                                this.helper.drawMyTexturedModalRect((float)drawX, (float)drawZ, 0, 64, 64.0F, 64.0F, -64.0F, 64.0F);
                                GL11.glTexParameteri(3553, 10240, 9728);
                                if (slimeChunks) {
                                    for(int t = 0; t < 16; ++t) {
                                        if (mchunk.getTile(t % 4, t / 4) != null && mchunk.getTile(t % 4, t / 4).isSlimeChunk()) {
                                            int slimeDrawX = drawX + 16 * (t % 4);
                                            int slimeDrawZ = drawZ + 16 * (t / 4);
                                            Gui.drawRect(slimeDrawX, slimeDrawZ, slimeDrawX + 16, slimeDrawZ + 16, -2142047936);
                                        }
                                    }
                                }
                                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                            }
                        }
                    }
                }
                GL14.glBlendFuncSeparate(770, 771, 1, 0);
            }
        }

        if (this.modMain.getSettings().chunkGrid > -1) {
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
            int grid = ModSettings.COLORS[this.modMain.getSettings().chunkGrid];
            int r = grid >> 16 & 0xFF;
            int g = grid >> 8 & 0xFF;
            int b = grid & 0xFF;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertexBuffer = tessellator.getBuffer();
            vertexBuffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            float red = (float)r / 255.0F;
            float green = (float)g / 255.0F;
            float blue = (float)b / 255.0F;
            float alpha = 0.8F;
            red *= chunkGridAlphaMultiplier;
            green *= chunkGridAlphaMultiplier;
            blue *= chunkGridAlphaMultiplier;
            GlStateManager.glLineWidth((float)this.modMain.getSettings().chunkGridLineWidth * Globals.minimapScalingFactor);
            int bias = (int)Math.ceil(this.zoom);

            for(int X = minX; X <= maxX; ++X) {
                int drawX = (X - playerChunkX + 1) * 64 - offsetX;

                for(int i = 0; i < 4; ++i) {
                    float lineX = (float)drawX + (float)(-16 * i);
                    this.helper
                            .addColoredLineToExistingBuffer(
                                    vertexBuffer, lineX, -((float)halfMaxVisibleLength), lineX, (float)halfMaxVisibleLength + (float)bias, red, green, blue, alpha
                            );
                }
            }

            for(int Z = minZ; Z <= maxZ; ++Z) {
                int drawZ = (Z - playerChunkZ + 1) * 64 - offsetZ;

                for(int i = 0; i < 4; ++i) {
                    float lineZ = (float)drawZ + (float)((double)(-16 * i) - 1.0 / this.zoom);
                    this.helper
                            .addColoredLineToExistingBuffer(
                                    vertexBuffer, -((float)halfMaxVisibleLength), lineZ, (float)halfMaxVisibleLength + (float)bias, lineZ, red, green, blue, alpha
                            );
                }
            }


            tessellator.draw();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        }

        this.scalingFramebuffer.unbindFramebuffer();
        this.rotationFramebuffer.bindFramebuffer(false);
        GL11.glClear(16640);
        this.scalingFramebuffer.bindFramebufferTexture();
        GlStateManager.loadIdentity();
        if (this.modMain.getSettings().getAntiAliasing()) {
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10241, 9729);
        } else {
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glTexParameteri(3553, 10241, 9728);
        }

        GlStateManager.translate(halfWView, halfWView, -2980.0F);
        GL11.glPushMatrix();
        if (!lockedNorth) {
            GL11.glRotatef(-angle, 0.0F, 0.0F, 1.0F);
        }

        GlStateManager.translate(-xInsidePixel * this.zoom, -zInsidePixel * this.zoom, 0.0);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, (float)(this.modMain.getSettings().minimapOpacity / 100.0));
        final float scaledSizeM = Globals.minimapScalingFactor * 512f;
        this.helper.drawMyTexturedModalRect(-scaledSize, -scaledSize, 0, 0, scaledSizeM, scaledSizeM, scaledSizeM);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
        GlStateManager.disableAlpha();
        GlStateManager.alphaFunc(516, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 1);
        GlStateManager.depthFunc(519);
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(false);
        GlStateManager.depthMask(true);
        GlStateManager.bindTexture(1);
        GlStateManager.bindTexture(0);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
        GlStateManager.pushMatrix();
        this.minimapElementMapRendererHandler
                .render(
                        renderEntity,
                        player,
                        playerX,
                        renderEntity.posY,
                        playerZ,
                        playerDimDiv,
                        ps,
                        pc,
                        this.zoom,
                        cave,
                        partial,
                        this.rotationFramebuffer,
                        this.modMain,
                        this.helper,
                        this.mc.fontRenderer,
                        scaledRes,
                        halfWView
                );
        GlStateManager.popMatrix();
        this.rotationFramebuffer.unbindFramebuffer();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.matrixMode(5889);
        Misc.minecraftOrtho(scaledRes);
        GlStateManager.matrixMode(5888);
        GL11.glPopMatrix();
    }

}
