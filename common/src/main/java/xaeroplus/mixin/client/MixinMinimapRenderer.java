package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
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
    public void resetFBOSize(
        CallbackInfo ci,
        @Local(argsOnly = true) MinimapProcessor minimap
        ) {
        if (this.minimap.usingFBO() && Globals.shouldResetFBO) {
            Globals.minimapScaleMultiplier = Settings.REGISTRY.minimapScaleMultiplierSetting.getAsInt();
            Globals.minimapSizeMultiplier = Settings.REGISTRY.minimapSizeMultiplierSetting.getAsInt();
            ((CustomMinimapFBORenderer) this.minimap.getMinimapFBORenderer()).reloadMapFrameBuffers();
            Globals.shouldResetFBO = false;
            minimap.setToResetImage(true);
        }
    }

    @Inject(method = "renderMinimap", at = @At("HEAD"))
    public void shiftRenderZHead(
        CallbackInfo ci,
        @Local(argsOnly = true) GuiGraphics guiGraphics
    ) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, Settings.REGISTRY.minimapRenderZOffsetSetting.get());
    }

    @Inject(method = "renderMinimap", at = @At("RETURN"))
    public void shiftRenderZPost(final MinimapSession minimapSession, final GuiGraphics guiGraphics, final MinimapProcessor minimap, final int x, final int y, final int width, final int height, final double scale, final int size, final float partial, final CustomVertexConsumers cvc, final CallbackInfo ci) {
        guiGraphics.pose().popPose();
    }

    @ModifyConstant(
        method = "renderMinimap",
        constant = @Constant(
            intValue = 256
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;DIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
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
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;DIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
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
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;DIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/common/graphics/CustomVertexConsumers;)V"
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
        target = "Lxaero/hud/minimap/element/render/over/MinimapElementOverMapRendererHandler;prepareRender(DDDIIIIZF)V"),
        remap = true) // $REMAP
    public void editOvermapRender(
        final MinimapElementOverMapRendererHandler instance,
        final double ps,
        final double pc,
        double zoom,
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
        instance.prepareRender(
               ps,
               pc,
               zoom,
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
    @WrapOperation(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;ZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"),
        remap = true) // $REMAP
    public void redirectRenderMainEntityDot(
        final MinimapFBORenderer instance, final GuiGraphics guiGraphics, final Entity renderEntity, final boolean cave, final MultiBufferSource.BufferSource renderTypeBuffers, final Operation<Void> original,
        @Local(name = "lockedNorth") boolean lockedNorth
    ) {
        if (Settings.REGISTRY.fixMainEntityDot.get()) {
            if (!(modMain.getSettings().mainEntityAs != 2 && !lockedNorth)) {
                return;
            }
        }
        original.call(instance, guiGraphics, renderEntity, cave, renderTypeBuffers);
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
