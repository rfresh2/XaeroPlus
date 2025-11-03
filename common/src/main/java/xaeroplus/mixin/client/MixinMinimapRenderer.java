package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.hud.minimap.Minimap;
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
    public void shiftRenderZPost(
        final CallbackInfo ci,
        @Local(argsOnly = true) GuiGraphics guiGraphics
    ) {
        guiGraphics.pose().popPose();
    }

    @ModifyExpressionValue(
        method = "renderMinimap",
        at = @At(
            value = "CONSTANT",
            args = "intValue=256"
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

    @ModifyExpressionValue(
        method = "renderMinimap",
        at = @At(
            value = "CONSTANT",
            args = "floatValue=256.0",
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

    @ModifyExpressionValue(
        method = "renderMinimap",
        at = @At(
            value = "CONSTANT",
            args = "floatValue=256.0",
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

    @ModifyArg(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/element/render/over/MinimapElementOverMapRendererHandler;prepareRender(DDDIIIIZF)V"
    ), index = 2)
    public double setOvermapRendererZoom(double zoom) {
        if (this.minimap.usingFBO()) {
            return (zoom / Globals.minimapScaleMultiplier) * Globals.minimapSizeMultiplier;
        }
        return zoom;
    }

    /**
     * Inspiration for the below mods came from: https://github.com/Abbie5/xaeroarrowfix
     */
    @WrapWithCondition(method = "renderMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;ZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"),
        remap = true) // $REMAP
    public boolean redirectRenderMainEntityDot(
        final MinimapFBORenderer instance, final GuiGraphics guiGraphics, final Entity renderEntity, final boolean cave, final MultiBufferSource.BufferSource renderTypeBuffers,
        @Local(name = "lockedNorth") boolean lockedNorth
    ) {
        if (Settings.REGISTRY.fixMainEntityDot.get()) {
            return modMain.getSettings().mainEntityAs != 2 && !lockedNorth;
        }
        return true;
    }

    @ModifyExpressionValue(
        method = "drawArrow",
        at = @At(
            value = "CONSTANT",
            args = "intValue=-6",
            ordinal = 0
        )
    )
    public int fixMainEntityDotOffset(final int original) {
        return Settings.REGISTRY.fixMainEntityDot.get() ? -10 : original;
    }
}
