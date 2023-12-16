package xaeroplus.mixin.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.MinimapInterface;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.common.minimap.radar.MinimapRadar;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.settings.ModSettings;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.CustomMinimapFBORenderer;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapRenderer {
    @Shadow
    protected MinimapInterface minimapInterface;
    @Shadow
    protected IXaeroMinimap modMain;

    @Inject(method = "renderMinimap", at = @At("HEAD"))
    public void renderMinimap(
            final XaeroMinimapSession minimapSession,
            final DrawContext guiGraphics,
            final MinimapProcessor minimap,
            final int x,
            final int y,
            final int width,
            final int height,
            final double scale,
            final int size,
            final float partial,
            final CustomVertexConsumers cvc,
            final CallbackInfo ci
    ) {
        if (this.minimapInterface.usingFBO() && Globals.shouldResetFBO) {
            Globals.minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
            ((CustomMinimapFBORenderer) this.minimapInterface.getMinimapFBORenderer()).reloadMapFrameBuffers();
            Globals.shouldResetFBO = false;
            minimap.setToResetImage(true);
        }
    }

    @Redirect(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/element/render/over/MinimapElementOverMapRendererHandler;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/player/PlayerEntity;DDDDDDDZFLnet/minecraft/client/gl/Framebuffer;Lxaero/common/IXaeroMinimap;Lxaero/common/minimap/render/MinimapRendererHelper;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/font/TextRenderer;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;IIIIZF)V"),
        remap = true)
    public void editOvermapRender(final MinimapElementOverMapRendererHandler instance,
                                  final DrawContext guiGraphics,
                                  final Entity renderEntity,
                                  final PlayerEntity player,
                                  final double renderX,
                                  final double renderY,
                                  final double renderZ,
                                  final double playerDimDiv,
                                  final double ps,
                                  final double pc,
                                  final double zoom,
                                  final boolean cave,
                                  final float partialTicks,
                                  final Framebuffer framebuffer,
                                  final IXaeroMinimap modMain,
                                  final MinimapRendererHelper helper,
                                  final VertexConsumerProvider.Immediate renderTypeBuffers,
                                  final TextRenderer font,
                                  final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                  final int specW,
                                  final int specH,
                                  final int halfViewW,
                                  final int halfViewH,
                                  final boolean circle,
                                  final float minimapScale
    ) {
        double customZoom = zoom / Globals.minimapScalingFactor;
        instance.render(
                guiGraphics,
                renderEntity,
                player,
                renderX,
                renderY,
                renderZ,
                playerDimDiv,
                ps,
                pc,
                customZoom,
                cave,
                partialTicks,
                framebuffer,
                modMain,
                helper,
                renderTypeBuffers,
                font,
                multiTextureRenderTypeRenderers,
                specW,
                specH,
                halfViewW,
                halfViewH,
                circle,
                minimapScale
        );
    }


    /**
     * Inspiration for the below mods came from: https://github.com/Abbie5/xaeroarrowfix
     */

    @Redirect(method = "renderMinimap", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lnet/minecraft/client/gui/DrawContext;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;DDDDFLxaero/common/minimap/radar/MinimapRadar;ZIZZZDLxaero/common/settings/ModSettings;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;F)V"), remap = true)
    public void redirectRenderMainEntityDot(final MinimapFBORenderer instance,
                                            final DrawContext guiGraphics,
                                            final MinimapProcessor minimap,
                                            final PlayerEntity p,
                                            final Entity renderEntity,
                                            final double ps,
                                            final double pc,
                                            final double playerX,
                                            final double playerZ,
                                            final float partial,
                                            final MinimapRadar minimapRadar,
                                            final boolean lockedNorth,
                                            final int style,
                                            final boolean smooth,
                                            final boolean debug,
                                            final boolean cave,
                                            final double dotNameScale,
                                            final ModSettings settings,
                                            final VertexConsumerProvider.Immediate renderTypeBuffers,
                                            final float minimapScale) {
        if (XaeroPlusSettingRegistry.fixMainEntityDot.getValue()) {
            if (!(modMain.getSettings().mainEntityAs != 2 && !lockedNorth)) {
                return;
            }
        }
        instance.renderMainEntityDot(
                guiGraphics,
                minimap,
                p,
                renderEntity,
                ps,
                pc,
                playerX,
                playerZ,
                partial,
                minimapRadar,
                lockedNorth,
                style,
                smooth,
                debug,
                cave,
                dotNameScale,
                settings,
                renderTypeBuffers,
                minimapScale
        );
    }

    @ModifyVariable(method = "drawArrow", name = "offsetY", ordinal = 0, at = @At(value = "STORE"))
    public int modifyArrowOffsetY(final int offsetY) {
        return XaeroPlusSettingRegistry.fixMainEntityDot.getValue() ? -10 : offsetY;
    }
}
