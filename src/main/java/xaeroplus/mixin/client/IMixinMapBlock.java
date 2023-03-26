package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.region.MapBlock;
import xaero.map.region.MapPixel;

@Mixin(MapBlock.class)
public interface IMixinMapBlock {

        @Accessor("slopeUnknown")
        boolean getSlopeUnknown();

        @Accessor("slopeUnknown")
        void setSlopeUnknown(boolean slopeUnknown);
}
