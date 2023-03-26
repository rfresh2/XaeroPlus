package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.region.MapPixel;

@Mixin(MapPixel.class)
public interface IMixinMapPixel {

    @Accessor("light")
    byte getLight();
}








