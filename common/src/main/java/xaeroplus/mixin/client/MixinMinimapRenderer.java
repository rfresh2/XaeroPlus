package xaeroplus.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.common.minimap.radar.MinimapRadar;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.settings.ModSettings;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.module.MinimapSession;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.CustomMinimapFBORenderer;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapRenderer {
    @Shadow
    protected Minimap minimap;
    @Shadow
    protected IXaeroMinimap modMain;

    @Inject(method = "renderMinimap", at = @At("HEAD"))
    public void renderMinimap(
        final MinimapSession minimapSession,
        final PoseStack matrixStack,
        final MinimapProcessor minimap,
        final int x,
        final int y,
        final int width,
        final int height,
        final double scale,
        final int size,
        final float partial,
        final CustomVertexConsumers cvc,
        final CallbackInfo ci
    ) {
        if (this.minimap.usingFBO() && Globals.shouldResetFBO) {
            Globals.minimapScaleMultiplier = (int) XaeroPlusSettingRegistry.minimapScaleMultiplierSetting.getValue();
            Globals.minimapSizeMultiplier = (int) XaeroPlusSettingRegistry.minimapSizeMultiplierSetting.getValue();
            ((CustomMinimapFBORenderer) this.minimap.getMinimapFBORenderer()).reloadMapFrameBuffers();
            Globals.shouldResetFBO = false;
            minimap.setToResetImage(true);
        }
    }

    @ModifyConstant(
        method = "renderMinimap",
        constant = @Constant(
            intValue = 256
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/MinimapProcessor;DDDDIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
            )
        )
    )
    public int modifyMinimapSizeConstantI(final int constant) {
        if (this.minimap.usingFBO()) {
            return constant * Globals.minimapSizeMultiplier;
        } else {
            return constant;
        }
    }

    @ModifyConstant(
        method = "renderMinimap",
        constant = @Constant(
            floatValue = 256.0f,
            ordinal = 0
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/MinimapProcessor;DDDDIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
            )
        )
    )
    public float modifyMinimapSizeConstantF(final float constant) {
        if (this.minimap.usingFBO()) {
            return constant * Globals.minimapSizeMultiplier;
        } else {
            return constant;
        }
    }

    @ModifyConstant(
        method = "renderMinimap",
        constant = @Constant(
            floatValue = 256.0f,
            ordinal = 1
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/MinimapProcessor;DDDDIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
            )
        )
    )
    public float modifyMinimapSizeConstantFCircle(final float constant) {
        if (this.minimap.usingFBO()) {
            return constant * Globals.minimapSizeMultiplier;
        } else {
            return constant;
        }
    }

    @Redirect(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/element/render/over/MinimapElementOverMapRendererHandler;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/player/Player;DDDDDDDZFLcom/mojang/blaze3d/pipeline/RenderTarget;Lxaero/common/IXaeroMinimap;Lxaero/common/minimap/render/MinimapRendererHelper;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/gui/Font;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;IIIIZF)V"),
        remap = true) // $REMAP
    public void editOvermapRender(final MinimapElementOverMapRendererHandler instance,
                                  final PoseStack guiGraphics,
                                  final Entity renderEntity,
                                  final Player player,
                                  final double renderX,
                                  final double renderY,
                                  final double renderZ,
                                  final double playerDimDiv,
                                  final double ps,
                                  final double pc,
                                  double zoom,
                                  final boolean cave,
                                  final float partialTicks,
                                  final RenderTarget framebuffer,
                                  final IXaeroMinimap modMain,
                                  final MinimapRendererHelper helper,
                                  final MultiBufferSource.BufferSource renderTypeBuffers,
                                  final Font font,
                                  final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                  final int specW,
                                  final int specH,
                                  final int halfViewW,
                                  final int halfViewH,
                                  final boolean circle,
                                  final float minimapScale
    ) {
        if (this.minimap.usingFBO()) {
            zoom = (zoom / Globals.minimapScaleMultiplier) * Globals.minimapSizeMultiplier;
        }
        instance.render(
                guiGraphics,
                renderEntity,
                player,
                renderX,
                renderY,
                renderZ,
                playerDimDiv,
                ps,
                pc,
                zoom,
                cave,
                partialTicks,
                framebuffer,
                modMain,
                helper,
                renderTypeBuffers,
                font,
                multiTextureRenderTypeRenderers,
                specW,
                specH,
                halfViewW,
                halfViewH,
                circle,
                minimapScale
        );
    }


    /**
     * Inspiration for the below mods came from: https://github.com/Abbie5/xaeroarrowfix
     */

    @Redirect(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;DDDDFLxaero/common/minimap/radar/MinimapRadar;ZIZZZDLxaero/common/settings/ModSettings;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;F)V"),
        remap = true) // $REMAP
    public void redirectRenderMainEntityDot(final MinimapFBORenderer instance,
                                            final PoseStack guiGraphics,
                                            final MinimapProcessor minimap,
                                            final Player p,
                                            final Entity renderEntity,
                                            final double ps,
                                            final double pc,
                                            final double playerX,
                                            final double playerZ,
                                            final float partial,
                                            final MinimapRadar minimapRadar,
                                            final boolean lockedNorth,
                                            final int style,
                                            final boolean smooth,
                                            final boolean debug,
                                            final boolean cave,
                                            final double dotNameScale,
                                            final ModSettings settings,
                                            final MultiBufferSource.BufferSource renderTypeBuffers,
                                            final float minimapScale) {
        if (XaeroPlusSettingRegistry.fixMainEntityDot.getValue()) {
            if (!(modMain.getSettings().mainEntityAs != 2 && !lockedNorth)) {
                return;
            }
        }
        instance.renderMainEntityDot(
                guiGraphics,
                minimap,
                p,
                renderEntity,
                ps,
                pc,
                playerX,
                playerZ,
                partial,
                minimapRadar,
                lockedNorth,
                style,
                smooth,
                debug,
                cave,
                dotNameScale,
                settings,
                renderTypeBuffers,
                minimapScale
        );
    }

    @ModifyVariable(method = "drawArrow", name = "offsetY", ordinal = 0, at = @At(value = "STORE"))
    public int modifyArrowOffsetY(final int offsetY) {
        return XaeroPlusSettingRegistry.fixMainEntityDot.getValue() ? -10 : offsetY;
    }
}
