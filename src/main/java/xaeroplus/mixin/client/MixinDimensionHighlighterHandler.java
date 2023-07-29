package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.highlight.AbstractHighlighter;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.highlight.HighlighterRegistry;

import java.util.List;

@Mixin(value = DimensionHighlighterHandler.class, remap = false)
public class MixinDimensionHighlighterHandler {

    @Final
    @Shadow private int dimension;
    @Final
    @Shadow private HighlighterRegistry registry;

    /**
     * @author rfresh2
     * @reason reduce object allocations
     */
    @Overwrite
    public boolean shouldApplyRegionHighlights(int regionX, int regionZ, boolean discovered) {
        int dimension = this.dimension;

        List<AbstractHighlighter> highlighters = this.registry.getHighlighters();
        for (int i = 0; i < highlighters.size(); i++) {
            AbstractHighlighter hl = highlighters.get(i);
            if ((discovered || hl.isCoveringOutsideDiscovered()) && hl.regionHasHighlights(dimension, regionX, regionZ)) {
                return true;
            }
        }
        return false;
    }

}
