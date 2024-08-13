package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.GuiMap;

@Mixin(value = GuiMap.class, remap = false)
public interface AccessorGuiMap {
    @Accessor(value = "cameraX")
    double getCameraX();

    @Accessor(value = "cameraZ")
    double getCameraZ();

    @Accessor(value = "destScale")
    static double getDestScale() {
        throw new AssertionError();
    }
}
