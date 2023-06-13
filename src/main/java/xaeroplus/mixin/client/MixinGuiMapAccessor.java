package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.GuiMap;

@Mixin(value = GuiMap.class)
public interface MixinGuiMapAccessor {
    @Accessor(value = "cameraX")
    double getCameraX();

    @Accessor(value = "cameraZ")
    double getCameraZ();

    @Accessor(value = "destScale")
    double getDestScale();
}
