package xaeroplus.mixin.client;

import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapInterface;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapRenderer;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapRenderer {

    @Shadow
    protected MinimapInterface minimapInterface;

    @Inject(method = "renderMinimap", at = @At("HEAD"))
    public void renderMinimap(
            final XaeroMinimapSession minimapSession, final MinimapProcessor minimap, final int x, final int y, final int width, final int height, final ScaledResolution scaledRes, final int size, final float partial, final CallbackInfo ci
    ) {
        if (this.minimapInterface.usingFBO() && XaeroPlus.shouldResetFBO) {
            this.minimapInterface.getMinimapFBORenderer().deleteFramebuffers();
            this.minimapInterface.getMinimapFBORenderer().loadFrameBuffer(minimap);
            XaeroPlus.minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
            XaeroPlus.shouldResetFBO = false;
        }
    }
}
