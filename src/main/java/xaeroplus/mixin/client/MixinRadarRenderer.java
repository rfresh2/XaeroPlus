package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.element.render.MinimapElementReader;
import xaero.common.minimap.element.render.MinimapElementRenderProvider;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.radar.render.element.RadarRenderContext;
import xaero.hud.minimap.radar.render.element.RadarRenderer;
import xaeroplus.settings.Settings;

@Mixin(value = RadarRenderer.class, remap = false)
public abstract class MixinRadarRenderer extends MinimapElementRenderer<Entity, RadarRenderContext> {
    @Shadow
    private boolean name;

    public MixinRadarRenderer(final MinimapElementReader<Entity, RadarRenderContext> elementReader, final MinimapElementRenderProvider<Entity, RadarRenderContext> provider, final RadarRenderContext context) {
        super(elementReader, provider, context);
    }

    @Inject(method = "setupRenderForEntity", at = @At("RETURN"))
    public void forceEntityRadarRenderSettings(final Entity e, final CallbackInfo ci) {
        if (!(e instanceof EntityPlayer)) return;
        if (e == Minecraft.getMinecraft().player) return;
        if (Settings.REGISTRY.alwaysRenderPlayerIconOnRadar.getValue()) {
            context.icon = true;
        }
        if (Settings.REGISTRY.alwaysRenderPlayerWithNameOnRadar.getValue()) {
            this.name = true;
        }
    }
}
