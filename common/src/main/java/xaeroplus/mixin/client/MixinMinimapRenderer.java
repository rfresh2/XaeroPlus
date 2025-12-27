package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.HudMod;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.CustomMinimapFBORenderer;
import xaeroplus.settings.Settings;
import xaeroplus.util.GuiMapHelper;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapRenderer {
    @Shadow
    protected Minimap minimap;
    @Final @Shadow
    protected PoseStack matrixStack;

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

    @Inject(method = "renderMinimap", at = @At("HEAD"), cancellable = true)
    public void cancelMinimapRenderWhileInTransparentWorldmap(final CallbackInfo ci) {
        if (GuiMapHelper.isGuiMapLoaded()) ci.cancel();
    }

//    @Inject(method = "renderMinimap", at = @At("HEAD"))
//    public void shiftRenderZHead(
//        CallbackInfo ci,
//        @Local(argsOnly = true) GuiGraphics guiGraphics
//    ) {
//        guiGraphics.pose().pushMatrix();
//        // todo: can we even shift z like this anymore???
//        guiGraphics.pose().translate(0, 0); // , Settings.REGISTRY.minimapRenderZOffsetSetting.get());
//    }

//    @Inject(method = "renderMinimap", at = @At("RETURN"))
//    public void shiftRenderZPost(
//        final CallbackInfo ci,
//        @Local(argsOnly = true) GuiGraphics guiGraphics
//    ) {
//        guiGraphics.pose().popPose();
//    }

    @ModifyExpressionValue(
        method = "renderMinimap",
        at = @At(
            value = "CONSTANT",
            args = "intValue=256"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;DIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/lib/client/graphics/XaeroBufferProvider;)V"
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
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;DIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/lib/client/graphics/XaeroBufferProvider;)V"
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
                target = "Lxaero/common/minimap/render/MinimapRenderer;renderChunks(Lxaero/hud/minimap/module/MinimapSession;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/resources/ResourceKey;DIIFFIZZIDDZZLxaero/common/settings/ModSettings;Lxaero/lib/client/graphics/XaeroBufferProvider;)V"
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
        target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lnet/minecraft/world/entity/Entity;ZLxaero/lib/client/graphics/XaeroBufferProvider;)V"),
        remap = true) // $REMAP
    public boolean redirectRenderMainEntityDot(
        final MinimapFBORenderer instance, final Entity renderEntity, final boolean cave, final XaeroBufferProvider renderTypeBuffers,
        @Local(name = "lockedNorth") boolean lockedNorth
    ) {
        if (Settings.REGISTRY.fixMainEntityDot.get()) {
            return HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getEffective(MinimapProfiledConfigOptions.RADAR_MAIN_ENTITY) != 2 && !lockedNorth;
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
