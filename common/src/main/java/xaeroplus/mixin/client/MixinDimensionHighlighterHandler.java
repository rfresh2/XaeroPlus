package xaeroplus.mixin.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.highlight.HighlighterRegistry;

@Mixin(value = DimensionHighlighterHandler.class)
public class MixinDimensionHighlighterHandler {

    @Final
    @Shadow
    private ResourceKey<Level> dimension;
    @Final
    @Shadow
    private HighlighterRegistry registry;

    /**
     * @author rfresh2
     * @reason Optimize excessive iterator allocations
     */
    @Overwrite
    public boolean shouldApplyRegionHighlights(int regionX, int regionZ, boolean discovered) {
        ResourceKey<Level> dimension = this.dimension;

        var highlighters = this.registry.getHighlighters();
        for (int i = 0; i < highlighters.size(); i++) {
            var hl = highlighters.get(i);
            if ((discovered || hl.isCoveringOutsideDiscovered()) && hl.regionHasHighlights(dimension, regionX, regionZ)) {
                return true;
            }
        }
        return false;
    }

}
