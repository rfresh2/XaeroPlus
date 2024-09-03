package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.radar.MinimapRadarList;
import xaero.common.minimap.radar.category.setting.EntityRadarCategorySettings;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderProvider;
import xaero.hud.HudSession;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.Globals;
import xaeroplus.util.IScreenRadarRenderContext;

import java.util.Objects;

@Mixin(value = RadarRenderProvider.class, remap = false)
public class MixinRadarRenderProvider {

    @Shadow
    private MinimapRadarList currentList;

    @Inject(method = "setupContextAndGetNext(ILxaero/common/minimap/render/radar/element/RadarRenderContext;)Lnet/minecraft/entity/Entity;",
            at = @At(value = "RETURN"))
    public void setupContextAndGetNextInject(final int location, final RadarRenderContext context, final CallbackInfoReturnable<Entity> cir) {
        final Entity e = cir.getReturnValue();
        if (e instanceof EntityPlayer) {
            if (!Objects.equals(e, Minecraft.getMinecraft().player)) {
                if (XaeroPlusSettingRegistry.alwaysRenderPlayerIconOnRadar.getValue()) {
                    context.icon = true;
                }
                if (XaeroPlusSettingRegistry.alwaysRenderPlayerWithNameOnRadar.getValue()) {
                    context.name = true;
                }
            }
        }

        if (!((IScreenRadarRenderContext) (Object) context).isWorldMap()) {
            context.nameScale = HudSession.getCurrentSession().getHudMod().getSettings().getDotNameScale() * Globals.minimapScalingFactor;
            context.iconScale = this.currentList.getCategory().getSettingValue(EntityRadarCategorySettings.ICON_SCALE) * Globals.minimapScalingFactor;
        }
    }
}
