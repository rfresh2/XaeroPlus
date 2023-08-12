package xaeroplus.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.IXaeroMinimap;
import xaero.common.MinimapLogs;
import xaero.common.XaeroMinimapSession;
import xaero.common.effect.Effects;
import xaero.common.graphics.CustomRenderTypes;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.graphics.ImprovedFramebuffer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.graphics.shader.MinimapShaders;
import xaero.common.minimap.MinimapInterface;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.element.render.map.MinimapElementMapRendererHandler;
import xaero.common.minimap.region.MinimapChunk;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.render.radar.EntityIconManager;
import xaero.common.minimap.render.radar.EntityIconPrerenderer;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaero.common.minimap.waypoints.render.CompassRenderer;
import xaero.common.minimap.waypoints.render.WaypointsGuiRenderer;
import xaero.common.misc.Misc;
import xaero.common.misc.OptimizedMath;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.*;

import static net.minecraft.world.World.NETHER;
import static net.minecraft.world.World.OVERWORLD;
import static xaeroplus.util.Shared.customDimensionId;

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

    public MixinMinimapFBORenderer(final IXaeroMinimap modMain, final MinecraftClient mc, final WaypointsGuiRenderer waypointsGuiRenderer, final MinimapInterface minimapInterface, final CompassRenderer compassRenderer) {
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
                this.scalingFramebuffer.delete();
            if (this.rotationFramebuffer != null)
                this.rotationFramebuffer.delete();
            // double the framebuffer size
            final int scaledSize = Shared.minimapScalingFactor * 512;
            this.scalingFramebuffer = new ImprovedFramebuffer(scaledSize, scaledSize, false);
            this.rotationFramebuffer = new ImprovedFramebuffer(scaledSize, scaledSize, true);
            this.rotationFramebuffer.setTexFilter(9729);
            this.loadedFBO = this.scalingFramebuffer.fbo != -1 && this.rotationFramebuffer.fbo != -1;
        }
    }

    public double getRenderEntityX(MinimapProcessor minimap, Entity renderEntity, float partial) {
        RegistryKey<World> dim = mc.world.getRegistryKey();
        // when player is in the nether or the custom dimension is the nether, perform coordinate translation
        if ((dim == NETHER || customDimensionId == NETHER) && dim != customDimensionId) {
            if (customDimensionId == OVERWORLD) {
                return minimap.getEntityRadar().getEntityX(renderEntity, partial) * 8.0;
            } else if (customDimensionId == NETHER && dim == OVERWORLD) {
                return minimap.getEntityRadar().getEntityX(renderEntity, partial) / 8.0;
            }
        }

        return minimap.getEntityRadar().getEntityX(renderEntity, partial);
    }

    public double getRenderEntityZ(MinimapProcessor minimap, Entity renderEntity, float partial) {
        RegistryKey<World> dim = mc.world.getRegistryKey();
        // when player is in the nether or the custom dimension is the nether, perform coordinate translation
        if ((dim == NETHER || customDimensionId == NETHER) && dim != customDimensionId) {
            if (customDimensionId == OVERWORLD) {
                return minimap.getEntityRadar().getEntityZ(renderEntity, partial) * 8.0;
            } else if (customDimensionId == NETHER && dim == OVERWORLD) {
                return minimap.getEntityRadar().getEntityZ(renderEntity, partial) / 8.0;
            }
        }

        return minimap.getEntityRadar().getEntityZ(renderEntity, partial);
    }

    /**
     * @author rfresh2
     * @reason big minimap
     */
    @Overwrite
    public void renderChunksToFBO(
            XaeroMinimapSession minimapSession,
            DrawContext guiGraphics,
            MinimapProcessor minimap,
            PlayerEntity player,
            Entity renderEntity,
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
            CustomVertexConsumers cvc
    ) {
        viewW *= Shared.minimapScalingFactor;
        final int scaledSize = 256 * Shared.minimapScalingFactor;
        MatrixStack matrixStack = guiGraphics.getMatrices();
        MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers = minimapSession.getMultiTextureRenderTypeRenderers();
        double maxVisibleLength = !lockedNorth && shape != 1 ? (double)viewW * Math.sqrt(2.0) : (double)viewW;
        double halfMaxVisibleLength = maxVisibleLength / 2.0;
        double radiusBlocks = maxVisibleLength / 2.0 / this.zoom;
        double playerX = getRenderEntityX(minimap, renderEntity, partial);
        double playerZ = getRenderEntityZ(minimap, renderEntity, partial);
        int xFloored = OptimizedMath.myFloor(playerX);
        int zFloored = OptimizedMath.myFloor(playerZ);
        int playerChunkX = xFloored >> 6;
        int playerChunkZ = zFloored >> 6;
        int offsetX = xFloored & 63;
        int offsetZ = zFloored & 63;
        boolean zooming = (double)((int)this.zoom) != this.zoom;
        this.scalingFramebuffer.beginWrite(true);
        GL11.glClear(16640);
        DiffuseLighting.disableGuiDepthLighting();
        long before = System.currentTimeMillis();
        GlStateManager._clear(256, MinecraftClient.IS_SYSTEM_MAC);
        this.helper.defaultOrtho(this.scalingFramebuffer, false);
        MatrixStack shaderMatrixStack = RenderSystem.getModelViewStack();
        shaderMatrixStack.push();
        shaderMatrixStack.loadIdentity();
        before = System.currentTimeMillis();
        double xInsidePixel = getRenderEntityX(minimap, renderEntity, partial) - (double)xFloored;
        if (xInsidePixel < 0.0) {
            ++xInsidePixel;
        }

        double zInsidePixel = getRenderEntityZ(minimap, renderEntity, partial) - (double)zFloored;
        if (zInsidePixel < 0.0) {
            ++zInsidePixel;
        }

        float halfWView = (float)viewW / 2.0F;
        float angle = (float)(90.0 - this.getRenderAngle(lockedNorth));
        RenderSystem.enableBlend();
        shaderMatrixStack.translate(scaledSize, scaledSize, -2000.0F);
        shaderMatrixStack.scale((float)this.zoom, (float)this.zoom, 1.0F);
        RenderSystem.applyModelViewMatrix();
        if (!XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) {
            guiGraphics.fill(-scaledSize, -scaledSize, scaledSize, scaledSize, ColorHelper.getColor(0, 0, 0, 255));
        } else {
            guiGraphics.fill(-scaledSize, -scaledSize, scaledSize, scaledSize, ColorHelper.getColor(0, 0, 0, 0));
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        VertexConsumerProvider.Immediate renderTypeBuffers = cvc.getBetterPVPRenderTypeBuffers();
        VertexConsumer overlayBufferBuilder = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_CHUNK_OVERLAY);
        float chunkGridAlphaMultiplier = 1.0F;
        int minX = playerChunkX + (int)Math.floor(((double)offsetX - radiusBlocks) / 64.0);
        int minZ = playerChunkZ + (int)Math.floor(((double)offsetZ - radiusBlocks) / 64.0);
        int maxX = playerChunkX + (int)Math.floor(((double)(offsetX + 1) + radiusBlocks) / 64.0);
        int maxZ = playerChunkZ + (int)Math.floor(((double)(offsetZ + 1) + radiusBlocks) / 64.0);
        if (!cave || !this.mc.player.hasStatusEffect(Effects.NO_CAVE_MAPS) && !this.mc.player.hasStatusEffect(Effects.NO_CAVE_MAPS_HARMFUL)) {
            if (useWorldMap) {
                chunkGridAlphaMultiplier = this.modMain.getSupportMods().worldmapSupport.getMinimapBrightness();
                ((CustomSupportXaeroWorldMap) this.modMain
                        .getSupportMods()
                        .worldmapSupport)
                        .drawMinimapWithDrawContext(
                                guiGraphics,
                                minimapSession,
                                matrixStack,
                                this.getHelper(),
                                xFloored,
                                zFloored,
                                minX,
                                minZ,
                                maxX,
                                maxZ,
                                zooming,
                                this.zoom,
                                overlayBufferBuilder,
                                multiTextureRenderTypeRenderers
                        );
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
                MultiTextureRenderTypeRenderer multiTextureRenderTypeRenderer = multiTextureRenderTypeRenderers.getRenderer(
                        t -> RenderSystem.setShaderTexture(0, t), MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.GUI_BILINEAR
                );
                MinimapRendererHelper helper = this.getHelper();

                for(int X = minX; X <= maxX; ++X) {
                    int canvasX = X - minimap.getMinimapWriter().getLoadedMapChunkX();

                    for(int Z = minZ; Z <= maxZ; ++Z) {
                        int canvasZ = Z - minimap.getMinimapWriter().getLoadedMapChunkZ();
                        MinimapChunk mchunk = minimap.getMinimapWriter().getLoadedBlocks()[canvasX][canvasZ];
                        if (mchunk != null) {
                            int texture = mchunk.bindTexture(level);
                            if (mchunk.isHasSomething() && level < mchunk.getLevelsBuffered() && texture != 0) {
                                if (!zooming) {
                                    GL11.glTexParameteri(3553, 10240, 9728);
                                } else {
                                    GL11.glTexParameteri(3553, 10240, 9729);
                                }

                                int drawX = (X - playerChunkX) * 64 - offsetX;
                                int drawZ = (Z - playerChunkZ) * 64 - offsetZ;
                                helper.prepareMyTexturedColoredModalRect(
                                        matrixStack.peek().getPositionMatrix(),
                                        (float)drawX,
                                        (float)drawZ,
                                        0,
                                        64,
                                        64.0F,
                                        64.0F,
                                        -64.0F,
                                        64.0F,
                                        texture,
                                        1.0F,
                                        1.0F,
                                        1.0F,
                                        1.0F,
                                        multiTextureRenderTypeRenderer
                                );
                                if (slimeChunks) {
                                    for(int t = 0; t < 16; ++t) {
                                        if (mchunk.getTile(t % 4, t / 4) != null && mchunk.getTile(t % 4, t / 4).isSlimeChunk()) {
                                            int slimeDrawX = drawX + 16 * (t % 4);
                                            int slimeDrawZ = drawZ + 16 * (t / 4);
                                            helper.addColoredRectToExistingBuffer(
                                                    matrixStack.peek().getPositionMatrix(), overlayBufferBuilder, (float)slimeDrawX, (float)slimeDrawZ, 16, 16, -2142047936
                                            );
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                multiTextureRenderTypeRenderers.draw(multiTextureRenderTypeRenderer);
            }
        }

        if (this.modMain.getSettings().chunkGrid > -1) {
            VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_LINES);
            int grid = ModSettings.COLORS[this.modMain.getSettings().chunkGrid];
            int r = grid >> 16 & 0xFF;
            int g = grid >> 8 & 0xFF;
            int b = grid & 0xFF;
            MinimapShaders.FRAMEBUFFER_LINES.setFrameSize((float)this.scalingFramebuffer.viewportWidth, (float)this.scalingFramebuffer.viewportHeight);
            float red = (float)r / 255.0F;
            float green = (float)g / 255.0F;
            float blue = (float)b / 255.0F;
            float alpha = 0.8F;
            red *= chunkGridAlphaMultiplier;
            green *= chunkGridAlphaMultiplier;
            blue *= chunkGridAlphaMultiplier;
            RenderSystem.lineWidth((float)this.modMain.getSettings().chunkGridLineWidth * Shared.minimapScalingFactor);
            int bias = 1;
            MatrixStack.Entry matrices = matrixStack.peek();

            for(int X = minX; X <= maxX; ++X) {
                int drawX = (X - playerChunkX + 1) * 64 - offsetX;

                for(int i = 0; i < 4; ++i) {
                    float lineX = (float)drawX + (float)(-16 * i);
                    this.helper
                            .addColoredLineToExistingBuffer(
                                    matrices,
                                    lineBufferBuilder,
                                    lineX,
                                    -((float)halfMaxVisibleLength),
                                    lineX,
                                    (float)halfMaxVisibleLength + (float)bias,
                                    red,
                                    green,
                                    blue,
                                    alpha
                            );
                }
            }

            for(int Z = minZ; Z <= maxZ; ++Z) {
                int drawZ = (Z - playerChunkZ + 1) * 64 - offsetZ;

                for(int i = 0; i < 4; ++i) {
                    float lineZ = (float)drawZ + (float)((double)(-16 * i) - 1.0 / this.zoom);
                    this.helper
                            .addColoredLineToExistingBuffer(
                                    matrices,
                                    lineBufferBuilder,
                                    -((float)halfMaxVisibleLength),
                                    lineZ,
                                    (float)halfMaxVisibleLength + (float)bias,
                                    lineZ,
                                    red,
                                    green,
                                    blue,
                                    alpha
                            );
                }
            }
        }
        final boolean isDimensionSwitched = Shared.customDimensionId != MinecraftClient.getInstance().world.getRegistryKey();

        if (XaeroPlusSettingRegistry.showRenderDistanceSetting.getValue() && !isDimensionSwitched) {
            double actualPlayerX = minimap.getEntityRadar().getEntityX(mc.player, partial);
            double actualPlayerZ = minimap.getEntityRadar().getEntityZ(mc.player, partial);
            int actualXFloored = OptimizedMath.myFloor(actualPlayerX);
            int actualZFloored = OptimizedMath.myFloor(actualPlayerZ);
            final int setting = (int) XaeroPlusSettingRegistry.assumedServerRenderDistanceSetting.getValue();
            int width = setting * 2 + 1;
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
            MinimapShaders.FRAMEBUFFER_LINES.setFrameSize((float) scalingFramebuffer.viewportWidth, (float) scalingFramebuffer.viewportHeight);
            RenderSystem.lineWidth((float) modMain.getSettings().chunkGridLineWidth * Shared.minimapScalingFactor);
            MatrixStack.Entry matrices = matrixStack.peek();

            helper.addColoredLineToExistingBuffer(
                matrices,
                lineBufferBuilder,
                x0,
                z0,
                x1,
                z0,
                1.0f,
                1.0f,
                0.0f,
                0.8f
            );
            helper.addColoredLineToExistingBuffer(
                matrices,
                lineBufferBuilder,
                x1,
                z0,
                x1,
                z1,
                1.0f,
                1.0f,
                0.0f,
                0.8f
            );
            helper.addColoredLineToExistingBuffer(
                matrices,
                lineBufferBuilder,
                x1,
                z1,
                x0,
                z1,
                1.0f,
                1.0f,
                0.0f,
                0.8f
            );
            helper.addColoredLineToExistingBuffer(
                matrices,
                lineBufferBuilder,
                x0,
                z0,
                x0,
                z1,
                1.0f,
                1.0f,
                0.0f,
                0.8f
            );
        }

        renderTypeBuffers.draw();
        this.scalingFramebuffer.endWrite();
        this.rotationFramebuffer.beginWrite(false);
        GL11.glClear(16640);
        this.scalingFramebuffer.beginRead();
        shaderMatrixStack.loadIdentity();
        if (this.modMain.getSettings().getAntiAliasing()) {
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10241, 9729);
        } else {
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glTexParameteri(3553, 10241, 9728);
        }

        shaderMatrixStack.translate(halfWView, halfWView, -2980.0F);
        shaderMatrixStack.push();
        if (!lockedNorth) {
            OptimizedMath.rotatePose(shaderMatrixStack, -angle, OptimizedMath.ZP);
        }

        shaderMatrixStack.translate(-xInsidePixel * this.zoom, -zInsidePixel * this.zoom, 0.0);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)(this.modMain.getSettings().minimapOpacity / 100.0));
        final float scaledSizeM = Shared.minimapScalingFactor * 512f;
        this.helper.drawMyTexturedModalRect(matrixStack, -scaledSize, -scaledSize, 0, 0, scaledSizeM, scaledSizeM, scaledSizeM, scaledSizeM);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        shaderMatrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableBlend();
        RenderSystem.blendFuncSeparate(770, 771, 1, 1);
        GlStateManager._depthFunc(519);
        GlStateManager._depthFunc(515);
        GlStateManager._depthMask(false);
        GlStateManager._depthMask(true);
        GL11.glBindTexture(3553, 0);
        GlStateManager._bindTexture(0);
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 771);
        matrixStack.push();
        this.minimapElementMapRendererHandler
                .render(
                        guiGraphics,
                        renderEntity,
                        player,
                        playerX,
                        renderEntity.getY(),
                        playerZ,
                        ps,
                        pc,
                        this.zoom,
                        cave,
                        partial,
                        this.rotationFramebuffer,
                        this.modMain,
                        this.helper,
                        renderTypeBuffers,
                        this.mc.textRenderer,
                        multiTextureRenderTypeRenderers,
                        halfWView
                );
        matrixStack.pop();
        renderTypeBuffers.draw();
        this.rotationFramebuffer.endWrite();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        Misc.minecraftOrtho(this.mc, false);
        shaderMatrixStack.pop();
        RenderSystem.applyModelViewMatrix();
    }

}
