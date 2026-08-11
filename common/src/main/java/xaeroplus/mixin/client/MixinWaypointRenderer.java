package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.icon.XaeroIcon;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointRenderer;
import xaero.map.mods.gui.WaypointSymbolCreator;
import xaeroplus.settings.Settings;

@Mixin(value = WaypointRenderer.class, remap = false)
public class MixinWaypointRenderer {

    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/mods/gui/Waypoint;getAlpha()F"
    ))
    public float disableAlphaAnimForLongWp(final Waypoint instance, final Operation<Float> original) {
        if (Settings.REGISTRY.longWaypointInitials.get() && instance.getSymbol().length() > 2) {
            // basically avoids the alpha animation on hover
            // as our long wp will render with the same path as a hovered wp
            return 255.0f;
        }
        return original.call(instance);
    }

    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/mods/gui/WaypointSymbolCreator;getSymbolTexture(Ljava/lang/String;)Lxaero/map/icon/XaeroIcon;"
    ))
    public XaeroIcon wrapSymbolIconGen(
        final WaypointSymbolCreator instance,
        final String c,
        final Operation<XaeroIcon> original
    ) {
        if (Settings.REGISTRY.longWaypointInitials.get() && c.length() > 2) {
            // avoids the codepath where normal non-hovered waypoints render a texture
            return null;
        }
        return original.call(instance, c);
    }

    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/mods/gui/Waypoint;getName()Ljava/lang/String;"
    ))
    public String longWpRenderSymbolTextNonHovered(
        final Waypoint instance,
        final Operation<String> original,
        @Local(argsOnly = true) boolean hovered
    ) {
        if (Settings.REGISTRY.longWaypointInitials.get() && instance.getSymbol().length() > 2 && !hovered) {
            return instance.getSymbol();
        }
        return original.call(instance);
    }
}
