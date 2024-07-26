package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.radar.MinimapRadarList;
import xaero.common.minimap.radar.category.setting.EntityRadarCategorySettings;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderProvider;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.IScreenRadarRenderContext;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.Objects;

@Mixin(value = RadarRenderProvider.class, remap = false)
public class MixinRadarRenderProvider {

    @Shadow
    private MinimapRadarList currentList;

    @Inject(method = "setupContextAndGetNext(ILxaero/common/minimap/render/radar/element/RadarRenderContext;)Lnet/minecraft/world/entity/Entity;", at = @At(
        value = "RETURN"
    ), remap = true) // $REMAP
    public void setupContextAndGetNextInject(final int location, final RadarRenderContext context, final CallbackInfoReturnable<Entity> cir) {
        final Entity e = cir.getReturnValue();
        if (e instanceof Player) {
            if (!Objects.equals(e, Minecraft.getInstance().player)) {
                if (XaeroPlusSettingRegistry.alwaysRenderPlayerIconOnRadar.getValue()) {
                    context.icon = true;
                }
                if (XaeroPlusSettingRegistry.alwaysRenderPlayerWithNameOnRadar.getValue()) {
                    context.name = true;
                }
            }
        }

        if (!((IScreenRadarRenderContext) (Object) context).isWorldMap()) {
            context.nameScale = XaeroMinimapSession.getCurrentSession().getModMain().getSettings().getDotNameScale() * Globals.minimapScaleMultiplier / Globals.minimapSizeMultiplier;
            context.iconScale = this.currentList.getCategory().getSettingValue(EntityRadarCategorySettings.ICON_SCALE) * Globals.minimapScaleMultiplier / Globals.minimapSizeMultiplier;
        }
    }
}
