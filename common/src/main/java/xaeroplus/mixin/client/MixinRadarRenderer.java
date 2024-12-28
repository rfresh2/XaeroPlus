package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.element.render.MinimapElementReader;
import xaero.common.minimap.element.render.MinimapElementRenderProvider;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.radar.render.element.RadarRenderContext;
import xaero.hud.minimap.radar.render.element.RadarRenderer;
import xaeroplus.Globals;
import xaeroplus.settings.Settings;

@Mixin(value = RadarRenderer.class, remap = false)
public abstract class MixinRadarRenderer extends MinimapElementRenderer<Entity, RadarRenderContext> {
    @Shadow private boolean name;

    public MixinRadarRenderer(final MinimapElementReader<Entity, RadarRenderContext> elementReader, final MinimapElementRenderProvider<Entity, RadarRenderContext> provider, final RadarRenderContext context) {
        super(elementReader, provider, context);
    }

    @Inject(method = "setupRenderForEntity", at = @At("RETURN"),
        remap = true) // $REMAP
    public void forceEntityRadarRenderSettings(final Entity e, final CallbackInfo ci) {
        if (!(e instanceof Player)) return;
        if (e == Minecraft.getInstance().player) return;
        if (Settings.REGISTRY.alwaysRenderPlayerIconOnRadar.get()) {
            context.icon = true;
        }
        if (Settings.REGISTRY.alwaysRenderPlayerWithNameOnRadar.get()) {
            this.name = true;
        }
    }

    @Inject(method = "renderElement(Lnet/minecraft/world/entity/Entity;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z", at = @At("HEAD"),
        remap = true) // $REMAP
    public void adjustElementScaleForMinimapScaling(final CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalRef<MinimapElementRenderInfo> renderInfoRef, @Local(argsOnly = true) LocalFloatRef optionalScaleRef) {
        if (renderInfoRef.get().location == MinimapElementRenderLocation.IN_MINIMAP) {
            optionalScaleRef.set(optionalScaleRef.get() * Globals.minimapScaleMultiplier);
        }
    }
}
