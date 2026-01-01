package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.HudMod;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.hud.minimap.module.MinimapSession;
import xaeroplus.feature.extensions.CustomMinimapFBORenderer;
import xaeroplus.settings.Settings;
import xaeroplus.util.Globals;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapRenderer {

    @Shadow
    protected Minimap minimap;

    @Inject(method = "renderMinimap", at = @At("HEAD"))
    public void renderMinimap(
        final MinimapSession minimapSession, final MinimapProcessor minimap, final int x, final int y, final int width, final int height, final ScaledResolution scaledRes, final int size, final float partial, final CallbackInfo ci
    ) {
        if (this.minimap.usingFBO() && Globals.shouldResetFBO) {
            Globals.minimapScalingFactor = (int) Settings.REGISTRY.minimapScaling.getValue();
            ((CustomMinimapFBORenderer) this.minimap.getMinimapFBORenderer()).reloadMapFrameBuffers();
            Globals.shouldResetFBO = false;
        }
    }

    @Redirect(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/element/render/over/MinimapElementOverMapRendererHandler;prepareRender(DDDIIIIZF)V"
    ))
    public void editOvermapRender(
        final MinimapElementOverMapRendererHandler instance,
        double ps,
        double pc,
        double zoom,
        int specW,
        int specH,
        int halfViewW,
        int halfViewH,
        boolean circle,
        float minimapScale) {
        double customZoom = zoom / Globals.minimapScalingFactor;
        instance.prepareRender(
                ps,
                pc,
                customZoom,
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

    @WrapOperation(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lnet/minecraft/entity/Entity;ZLnet/minecraft/client/gui/ScaledResolution;)V"))
    public void redirectRenderMainEntityDot(MinimapFBORenderer instance, Entity entity, boolean cave, ScaledResolution scaledRes, final Operation<Void> original,
        @Local(name = "lockedNorth") boolean lockedNorth
    ) {
        if (Settings.REGISTRY.fixMainEntityDot.getValue()) {
            if (!(HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getEffective(MinimapProfiledConfigOptions.RADAR_MAIN_ENTITY) != 2 && !lockedNorth)) {
                return;
            }
        }
        original.call(instance, entity, cave, scaledRes);
    }

    @ModifyVariable(method = "drawArrow", name = "offsetY", ordinal = 0, at = @At(value = "STORE"))
    public int modifyArrowOffsetY(final int offsetY) {
        return Settings.REGISTRY.fixMainEntityDot.getValue() ? -10 : offsetY;
    }

}
