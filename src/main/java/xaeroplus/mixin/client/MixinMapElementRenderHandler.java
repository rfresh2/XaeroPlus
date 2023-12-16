package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.TextureManager;
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
            target = "Lxaero/map/element/MapElementRenderer;beforeRender(ILnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/DrawContext;DDDDFDDLnet/minecraft/client/texture/TextureManager;Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;Z)V"
        ), remap = true)
    public void renderWithRendererInject(MapElementRenderer renderer,
                                         Object hovered,
                                         GuiMap mapScreen,
                                         DrawContext guiGraphics,
                                         VertexConsumerProvider.Immediate renderTypeBuffers,
                                         MultiTextureRenderTypeRendererProvider rendererProvider,
                                         TextureManager textureManager,
                                         TextRenderer font,
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
                                         MinecraftClient mc,
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
