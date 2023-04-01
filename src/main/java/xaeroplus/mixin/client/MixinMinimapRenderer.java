package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapInterface;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.radar.MinimapRadar;
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

    @Redirect(method = "renderMinimap", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/radar/MinimapRadar;getEntityX(Lnet/minecraft/entity/Entity;F)D"))
    public double getEntityX(final MinimapRadar instance, final Entity e, final float partial) {
        return getPlayerX();
    }

    @Redirect(method = "renderMinimap", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/radar/MinimapRadar;getEntityZ(Lnet/minecraft/entity/Entity;F)D"))
    public double getEntityZ(final MinimapRadar instance, final Entity e, final float partial) {
        return getPlayerZ();
    }

    public double getPlayerX() {
        int dim = Minecraft.getMinecraft().world.provider.getDimension();
        // when player is in the nether or the custom dimension is the nether, perform coordinate translation
        if ((dim == -1 || XaeroPlus.customDimensionId == -1) && dim != XaeroPlus.customDimensionId) {
            if (XaeroPlus.customDimensionId == 0) {
                return Minecraft.getMinecraft().player.posX * 8.0;
            } else if (XaeroPlus.customDimensionId == -1 && dim == 0) {
                return Minecraft.getMinecraft().player.posX / 8.0;
            }
        }

        return Minecraft.getMinecraft().player.posX;
    }

    public double getPlayerZ() {
        int dim = Minecraft.getMinecraft().world.provider.getDimension();
        // when player is in the nether or the custom dimension is the nether, perform coordinate translation
        if ((dim == -1 || XaeroPlus.customDimensionId == -1) && dim != XaeroPlus.customDimensionId) {
            if (XaeroPlus.customDimensionId == 0) {
                return Minecraft.getMinecraft().player.posZ * 8.0;
            } else if (XaeroPlus.customDimensionId == -1 && dim == 0) {
                return Minecraft.getMinecraft().player.posZ / 8.0;
            }
        }

        return Minecraft.getMinecraft().player.posZ;
    }
}
