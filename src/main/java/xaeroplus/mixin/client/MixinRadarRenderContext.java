package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaeroplus.feature.extensions.IScreenRadarRenderContext;

@Mixin(value = RadarRenderContext.class, remap = false)
public class MixinRadarRenderContext implements IScreenRadarRenderContext {
    @Unique private boolean isWorldMap = false;

    @Override
    public boolean isWorldMap() {
        return isWorldMap;
    }

    @Override
    public void setIsWorldMap(final boolean isWorldMap) {
        this.isWorldMap = isWorldMap;
    }
}
