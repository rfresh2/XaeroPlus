package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.gui.GuiAddWaypoint;
import xaeroplus.settings.Settings;

@Mixin(value = GuiAddWaypoint.class, remap = false)
public class MixinGuiAddWaypoint {
    @ModifyExpressionValue(
        method = "checkFields",
        at = @At(
            value = "CONSTANT",
            args = "intValue=2"
        )
    )
    public int allowLongerInitials(int original) {
        return Settings.REGISTRY.longWaypointInitials.get()
            ? 100
            : original;
    }
}
