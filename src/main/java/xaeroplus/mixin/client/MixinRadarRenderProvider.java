package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = RadarRenderProvider.class, remap = false)
public class MixinRadarRenderProvider {

    @Inject(method = "setupContextAndGetNext(Lxaero/hud/minimap/element/render/MinimapElementRenderLocation;Lxaero/common/minimap/render/radar/element/RadarRenderContext;)Lnet/minecraft/entity/Entity;",
            at = @At(value = "RETURN"))
    public void forceEntityRadarRenderSettings(final MinimapElementRenderLocation location, final RadarRenderContext context, final CallbackInfoReturnable<Entity> cir) {
        final Entity e = cir.getReturnValue();
        if (!(e instanceof EntityPlayer)) return;
        if (e == Minecraft.getMinecraft().player) return;
        if (XaeroPlusSettingRegistry.alwaysRenderPlayerIconOnRadar.getValue()) {
            context.icon = true;
        }
        if (XaeroPlusSettingRegistry.alwaysRenderPlayerWithNameOnRadar.getValue()) {
            context.name = true;
        }
    }
}
