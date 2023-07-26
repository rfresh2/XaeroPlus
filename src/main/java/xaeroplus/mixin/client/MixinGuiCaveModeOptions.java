package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.gui.GuiCaveModeOptions;
import xaero.map.gui.GuiMap;
import xaero.map.world.MapDimension;
import xaeroplus.util.Shared;

@Mixin(value = GuiCaveModeOptions.class, remap = false)
public class MixinGuiCaveModeOptions {

    @Redirect(method = "onInit", at = @At(value = "INVOKE", target = "Lxaero/map/gui/GuiMap;getDimension()Lxaero/map/world/MapDimension;"))
    public MapDimension redirectGetDimension(final GuiMap instance) {
        return instance.getMapProcessor().getMapWorld().getDimension(Shared.customDimensionId);
    }
}
