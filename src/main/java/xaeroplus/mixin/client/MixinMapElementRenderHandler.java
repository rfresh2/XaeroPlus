package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.element.MapElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.GuiMap;
import xaeroplus.feature.extensions.IScreenRadarRenderContext;

@Mixin(value = MapElementRenderHandler.class, remap = false)
public class MixinMapElementRenderHandler {

    @Inject(
        method = "renderWithRenderer",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/map/element/MapElementRenderer;beforeRender(ILnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/GuiGraphics;DDDDFDDLnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;Z)V"
        ), remap = false)
    public void renderWithRendererInject(MapElementRenderer renderer,
                                         Object hovered,
                                         GuiMap mapScreen,
                                         GuiGraphics guiGraphics,
                                         MultiBufferSource.BufferSource renderTypeBuffers,
                                         MultiTextureRenderTypeRendererProvider rendererProvider,
                                         TextureManager textureManager,
                                         Font font,
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
                                         int elementIndex,
                                         int indexLimit,
                                         CallbackInfoReturnable<Integer> cir,
                                         @Local(name = "context") LocalRef<Object> contextRef) {
        if (contextRef.get() instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) contextRef.get()).setIsWorldMap(true);
        }
    }
}
