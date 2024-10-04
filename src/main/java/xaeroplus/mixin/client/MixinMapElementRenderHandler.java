package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.element.MapElementRenderer;
import xaero.map.gui.GuiMap;
import xaeroplus.util.IScreenRadarRenderContext;

@Mixin(value = MapElementRenderHandler.class, remap = false)
public class MixinMapElementRenderHandler {

    @Inject(
        method = "renderWithRenderer",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/map/element/MapElementRenderer;beforeRender(ILnet/minecraft/client/Minecraft;DDDDFDDLnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/gui/ScaledResolution;Z)V"
        ))
    public void renderWithRendererInject(MapElementRenderer renderer,
                                         Object hovered,
                                         GuiMap mapScreen,
                                         TextureManager textureManager,
                                         FontRenderer font,
                                         double cameraX,
                                         double cameraZ,
                                         int width,
                                         int height,
                                         double screenSizeBasedScale,
                                         double baseScale,
                                         double scale,
                                         double playerDimDiv,
                                         double mouseX,
                                         double mouseZ,
                                         float brightness,
                                         boolean cave,
                                         HoveredMapElementHolder oldHovered,
                                         Minecraft mc,
                                         boolean pre,
                                         float partialTicks,
                                         ScaledResolution scaledRes,
                                         int elementIndex,
                                         int indexLimit,
                                         CallbackInfoReturnable<Integer> cir,
                                         @Local(name = "context") Object context) {
        if (context instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) context).setIsWorldMap(true);
        }
    }
}
