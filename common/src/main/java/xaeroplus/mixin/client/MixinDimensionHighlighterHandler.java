package xaeroplus.mixin.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.highlight.AbstractHighlighter;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.highlight.HighlighterRegistry;

@Mixin(value = DimensionHighlighterHandler.class, remap = false)
public class MixinDimensionHighlighterHandler {

    @Final @Shadow private ResourceKey<Level> dimension;
    @Final @Shadow private HighlighterRegistry registry;

    /**
     * @author rfresh2
     * @reason optimize excessive iterator allocations at low worldmap zooms
     */
    @Overwrite
    public boolean shouldApplyRegionHighlights(int regionX, int regionZ, boolean discovered) {
        var dimension = this.dimension;
        var highlighters = this.registry.getHighlighters();
        try {
            for (int i = 0; i < highlighters.size(); i++) {
                AbstractHighlighter hl = highlighters.get(i);
                if (!discovered && !hl.isCoveringOutsideDiscovered()) continue;
                if (hl.regionHasHighlights(dimension, regionX, regionZ)) return true;
            }
        } catch (final Exception e) {
            // fall through
        }
        return false;
    }
}
