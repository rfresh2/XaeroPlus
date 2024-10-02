package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.radar.MinimapRadar;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.settings.ModSettings;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.hud.minimap.module.MinimapSession;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.CustomMinimapFBORenderer;
import xaeroplus.settings.Settings;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapRenderer {
    @Shadow
    protected Minimap minimap;
    @Shadow
    protected IXaeroMinimap modMain;

    @Inject(method = "renderMinimap", at = @At("HEAD"))
    public void renderMinimap(
        final MinimapSession minimapSession,
        final GuiGraphics guiGraphics,
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
            Globals.minimapScaleMultiplier = Settings.REGISTRY.minimapScaleMultiplierSetting.getAsInt();
            Globals.minimapSizeMultiplier = Settings.REGISTRY.minimapSizeMultiplierSetting.getAsInt();
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
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;DDIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
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
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;DDIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
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
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;DDIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
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
        target = "Lxaero/hud/minimap/element/render/over/MinimapElementOverMapRendererHandler;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;DDDDZFLcom/mojang/blaze3d/pipeline/RenderTarget;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V"),
        remap = true) // $REMAP
    public void editOvermapRender(final MinimapElementOverMapRendererHandler instance,
                                  final GuiGraphics guiGraphics,
                                  final Entity entity,
                                  final Player player,
                                  final Vec3 renderPos,
                                  final double playerDimDiv,
                                  final double ps,
                                  final double pc,
                                  double zoom,
                                  final boolean cave,
                                  final float partialTicks,
                                  final RenderTarget framebuffer,
                                  final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers
    ) {
        if (this.minimap.usingFBO()) {
            zoom = (zoom / Globals.minimapScaleMultiplier) * Globals.minimapSizeMultiplier;
        }
        instance.render(
               guiGraphics,
               entity,
               player,
               renderPos,
               playerDimDiv,
               ps,
               pc,
               zoom,
               cave,
               partialTicks,
               framebuffer,
               multiTextureRenderTypeRenderers
        );
    }


    /**
     * Inspiration for the below mods came from: https://github.com/Abbie5/xaeroarrowfix
     */

    @Redirect(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;DDDDFLxaero/common/minimap/radar/MinimapRadar;ZIZZZDLxaero/common/settings/ModSettings;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;F)V"),
        remap = true) // $REMAP
    public void redirectRenderMainEntityDot(final MinimapFBORenderer instance,
                                            final GuiGraphics guiGraphics,
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
        if (Settings.REGISTRY.fixMainEntityDot.get()) {
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
        return Settings.REGISTRY.fixMainEntityDot.get() ? -10 : offsetY;
    }

    @WrapOperation(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"
    ), remap = true) // $REMAP
    public void correctBlendingForFpsLimiter(final GlStateManager.SourceFactor sourceFactor, final GlStateManager.DestFactor destFactor, final GlStateManager.SourceFactor sourceFactor2, final GlStateManager.DestFactor destFactor2, final Operation<Void> original) {
        if (Settings.REGISTRY.minimapFpsLimiter.get()) {
            // todo: when minimap opacity is not set to 100 this is slightly different than without fps limiter
            //  the minimap will appear more opaque and dim than it should be
            //  when we are rendering to our buffering render target the blending isn't exactly the same as our BG opacity is 0
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.SRC_COLOR,
                GlStateManager.DestFactor.ZERO
            );
        } else {
            original.call(sourceFactor, destFactor, sourceFactor2, destFactor2);
        }
    }
}
