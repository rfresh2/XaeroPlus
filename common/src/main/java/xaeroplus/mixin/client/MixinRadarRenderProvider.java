package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaeroplus.settings.Settings;

@Mixin(value = RadarRenderProvider.class, remap = false)
public class MixinRadarRenderProvider {
    @Inject(method = "setupContextAndGetNext(Lxaero/hud/minimap/element/render/MinimapElementRenderLocation;Lxaero/common/minimap/render/radar/element/RadarRenderContext;)Lnet/minecraft/world/entity/Entity;", at = @At(
        value = "RETURN"
    ), remap = true) // $REMAP
    public void forceEntityRadarRenderSettings(final MinimapElementRenderLocation location, final RadarRenderContext context, final CallbackInfoReturnable<Entity> cir) {
        final Entity e = cir.getReturnValue();
        if (!(e instanceof Player)) return;
        if (e == Minecraft.getInstance().player) return;
        if (Settings.REGISTRY.alwaysRenderPlayerIconOnRadar.get()) {
            context.icon = true;
        }
        if (Settings.REGISTRY.alwaysRenderPlayerWithNameOnRadar.get()) {
            context.name = true;
        }
    }
}
