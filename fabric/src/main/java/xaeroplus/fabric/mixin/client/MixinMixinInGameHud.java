package xaeroplus.fabric.mixin.client;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.LayeredDraw;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.HudMod;
import xaero.common.core.XaeroMinimapCore;

/**
 * For ImmediatelyFast hud batching render compatibility
 * Only affects Fabric (currently)
 * May also affect Forge whenever its released for 1.20.6
 */
@Mixin(value = Gui.class, priority = 1500)
public class MixinMixinInGameHud {

    @Final @Shadow private LayeredDraw layers;

    @TargetHandler(
        mixin = "xaero.common.mixin.MixinInGameHud",
        name = "onRenderHotbarAndDecorations"
    )
    @Redirect(
        method = "@MixinSquared:Handler",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/core/XaeroMinimapCore;isModLoaded()Z"
        )
    )
    public boolean disableXaeroRenderMixin() {
        return false;
    }

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/LayeredDraw;add(Lnet/minecraft/client/gui/LayeredDraw$Layer;)Lnet/minecraft/client/gui/LayeredDraw;",
            ordinal = 1
        )
    )
    public LayeredDraw addRenderLayerForMinimapRender(final LayeredDraw instance, final LayeredDraw.Layer layer, final Operation<LayeredDraw> original) {
        instance.add((guiGraphics, ticks) -> {
            if (XaeroMinimapCore.isModLoaded()) {
                HudMod.INSTANCE.getEvents().handleRenderGameOverlayEventPre(guiGraphics, ticks);
                HudMod.INSTANCE.getModEvents().handleRenderModOverlay(guiGraphics, ticks);
            }
        });
        return original.call(instance, layer);
    }
}
