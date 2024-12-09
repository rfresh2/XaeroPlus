package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xaero.common.gui.GuiAddWaypoint;
import xaeroplus.settings.Settings;

@Mixin(value = GuiAddWaypoint.class, remap = false)
public class MixinGuiAddWaypoint {
    @ModifyConstant(
        method = "checkFields",
        constant = @Constant(
            intValue = 2
        )
    )
    public int allowLongerInitials(int original) {
        return Settings.REGISTRY.longWaypointInitials.get()
            ? 100
            : original;
    }
}
