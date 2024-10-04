package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.IXaeroMinimap;
import xaero.common.graphics.ImprovedFramebuffer;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.render.CompassRenderer;
import xaero.common.minimap.waypoints.render.WaypointsGuiRenderer;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.MinimapLogs;
import xaero.hud.minimap.module.MinimapSession;
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
    private boolean loadedFBO;

    public MixinMinimapFBORenderer(IXaeroMinimap modMain, Minecraft mc, WaypointsGuiRenderer waypointsGuiRenderer, Minimap minimapInterface, CompassRenderer compassRenderer) {
        super(modMain, mc, waypointsGuiRenderer, minimapInterface, compassRenderer);
    }

    @WrapOperation(method = "loadFrameBuffer", at = @At(
        value = "NEW",
        target = "(IIZ)Lxaero/common/graphics/ImprovedFramebuffer;"
    ))
    public ImprovedFramebuffer cancelFrameBufferInit(final int width, final int height, final boolean useDepthIn, final Operation<ImprovedFramebuffer> original) {
        return null;
    }

    @Redirect(method = "loadFrameBuffer", at = @At(
        value = "FIELD",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;rotationFramebuffer:Lxaero/common/graphics/ImprovedFramebuffer;",
        opcode = Opcodes.PUTFIELD
    ))
    public void initCustomFrameBuffers(final MinimapFBORenderer instance, final ImprovedFramebuffer value) {
        reloadMapFrameBuffers();
    }

    @Override
    public void reloadMapFrameBuffers() {
        if (!BuiltInHudModules.MINIMAP.getCurrentSession().getProcessor().canUseFrameBuffer()) {
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

    @WrapOperation(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderChunksToFBO(Lxaero/hud/minimap/module/MinimapSession;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;DDDDIIFFIZZZIDDZZLnet/minecraft/client/gui/ScaledResolution;)V"
    ))
    public void adjustViewParams(final MinimapFBORenderer instance, final MinimapSession minimapSession, final MinimapProcessor minimap, final EntityPlayer player, final Entity renderEntity, final double playerX, final double playerZ, final double playerDimDiv, final double mapDimensionScale, final int bufferSize, final int viewW, final float sizeFix, final float partial, final int level, final boolean retryIfError, final boolean useWorldMap, final boolean lockedNorth, final int shape, final double ps, final double pc, final boolean cave, final boolean circle, final ScaledResolution scaledRes, final Operation<Void> original) {
        original.call(instance, minimapSession, minimap, player, renderEntity, playerX, playerZ, playerDimDiv, mapDimensionScale, bufferSize,
                      viewW * Globals.minimapScalingFactor,
                      sizeFix, partial, level, retryIfError, useWorldMap, lockedNorth, shape, ps, pc, cave, circle, scaledRes);
    }

    @WrapOperation(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V",
        ordinal = 0
    ), remap = true)
    public void adjustTranslationForMinimapScaling(final float x, final float y, final float z, final Operation<Void> original) {
        final float scaledSize = 256 * Globals.minimapScalingFactor;
        original.call(scaledSize, scaledSize, z);
    }

    @WrapOperation(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V",
        ordinal = 0
    ), remap = true)
    public void adjustMinimapBackgroundRect(final int left, final int top, final int right, final int bottom, final int color, final Operation<Void> original) {
        final int scaledSize = 256 * Globals.minimapScalingFactor;
        if (!XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) {
            original.call(-scaledSize, -scaledSize, scaledSize, scaledSize, ColorHelper.getColor(0, 0, 0, 255));
        } else {
            original.call(-scaledSize, -scaledSize, scaledSize, scaledSize, ColorHelper.getColor(0, 0, 0, 0));
        }
    }

    @WrapOperation(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/GlStateManager;glLineWidth(F)V"
    ), remap = true)
    public void adjustChunkGridLineWidth(final float width, final Operation<Void> original) {
        original.call(width * Globals.minimapScalingFactor);
    }

    @WrapOperation(method = "renderChunksToFBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapRendererHelper;drawMyTexturedModalRect(FFIIFFF)V"
    ))
    public void adjustMinimapScaledSizeRect(final MinimapRendererHelper instance, final float x, final float y, final int textureX, final int textureY, final float width, final float height, final float factor, final Operation<Void> original) {
        final float scaledSizeM = Globals.minimapScalingFactor * 512f;
        final float scaledSize = 256 * Globals.minimapScalingFactor;
        original.call(instance, -scaledSize, -scaledSize, textureX, textureY, scaledSizeM, scaledSizeM, scaledSizeM);
    }

}
