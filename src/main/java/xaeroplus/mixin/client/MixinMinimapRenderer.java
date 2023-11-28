package xaeroplus.mixin.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapInterface;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.common.minimap.radar.MinimapRadar;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.CustomMinimapFBORenderer;
import xaeroplus.util.Globals;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapRenderer {

    @Shadow
    protected MinimapInterface minimapInterface;
    @Shadow
    protected IXaeroMinimap modMain;
    @Shadow
    private double lastMapDimensionScale = 1.0;
    @Shadow
    private double lastPlayerDimDiv = 1.0;

    @Inject(method = "renderMinimap", at = @At("HEAD"))
    public void renderMinimap(
            final XaeroMinimapSession minimapSession, final MinimapProcessor minimap, final int x, final int y, final int width, final int height, final ScaledResolution scaledRes, final int size, final float partial, final CallbackInfo ci
    ) {
        if (this.minimapInterface.usingFBO() && Globals.shouldResetFBO) {
            Globals.minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
            ((CustomMinimapFBORenderer) this.minimapInterface.getMinimapFBORenderer()).reloadMapFrameBuffers();
            Globals.shouldResetFBO = false;
        }
    }

    @Redirect(method = "renderMinimap", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/element/render/over/MinimapElementOverMapRendererHandler;render(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/player/EntityPlayer;DDDDDDDZFLnet/minecraft/client/shader/Framebuffer;Lxaero/common/IXaeroMinimap;Lxaero/common/minimap/render/MinimapRendererHelper;Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/gui/ScaledResolution;IIIIZF)D"))
    public double editOvermapRender(final MinimapElementOverMapRendererHandler instance, final Entity renderEntity, final EntityPlayer player, final double renderX, final double renderY, final double renderZ, final double playerDimDiv, final double ps, final double pc, final double zoom, final boolean cave, final float partialTicks, final Framebuffer framebuffer, final IXaeroMinimap modMain, final MinimapRendererHelper helper, final FontRenderer font, final ScaledResolution scaledRes, final int specW, final int specH, final int halfViewW, final int halfViewH, final boolean circle, final float minimapScale) {
        double customZoom = zoom / Globals.minimapScalingFactor;
        return instance.render(
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
                null,
                modMain,
                helper,
                font,
                scaledRes,
                specW,
                specW,
                halfViewW,
                halfViewH,
                circle,
                minimapScale
        );
    }

    /**
     * Inspiration for the below mods came from: https://github.com/Abbie5/xaeroarrowfix
     */

    @Redirect(method = "renderMinimap", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;DDDDFLxaero/common/minimap/radar/MinimapRadar;ZIZZZDLxaero/common/settings/ModSettings;Lnet/minecraft/client/gui/ScaledResolution;F)V"))
    public void redirectRenderMainEntityDot(final MinimapFBORenderer instance,
                                            final MinimapProcessor minimap,
                                            final EntityPlayer p,
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
                                            final ScaledResolution scaledRes,
                                            final float minimapScale) {
        if (XaeroPlusSettingRegistry.fixMainEntityDot.getValue()) {
            if (!(modMain.getSettings().mainEntityAs != 2 && !lockedNorth)) {
                return;
            }
        }
        instance.renderMainEntityDot(
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
                scaledRes,
                minimapScale
        );
    }

    @ModifyVariable(method = "drawArrow", name = "offsetY", ordinal = 0, at = @At(value = "STORE"))
    public int modifyArrowOffsetY(final int offsetY) {
        return XaeroPlusSettingRegistry.fixMainEntityDot.getValue() ? -10 : offsetY;
    }

}
