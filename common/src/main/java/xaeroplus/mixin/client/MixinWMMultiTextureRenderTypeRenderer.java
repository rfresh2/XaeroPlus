package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaeroplus.Globals;

@Mixin(value = MultiTextureRenderTypeRenderer.class, remap = false)
public class MixinWMMultiTextureRenderTypeRenderer {

    @WrapOperation(method = "draw", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V"
    ))
    public void transparentWmBgSetBlend(final RenderType instance, final Operation<Void> original) {
        original.call(instance);

        if (Globals.transparentWmBgApplyGuiBilinearBlend) {
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            RenderSystem.blendEquation(GlConst.GL_FUNC_ADD);
        }
        Globals.transparentWmBgApplyGuiBilinearBlend = false;

        if (Globals.transparentWmBgApplyMapBlend) {
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.blendEquation(GlConst.GL_FUNC_ADD);
        }
        Globals.transparentWmBgApplyMapBlend = false;
    }
}
