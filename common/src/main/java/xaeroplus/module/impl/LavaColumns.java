package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xaeroplus.Globals;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.feature.highlights.SavableHighlightCacheInstance;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.module.Module;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.concurrent.atomic.AtomicInteger;

import static xaeroplus.util.ChunkUtils.getActualDimension;

public class LavaColumns extends Module {
    public final SavableHighlightCacheInstance lavaColumnsCache = new SavableHighlightCacheInstance("XaeroPlusLavaColumns");
    private int minColumnHeight = 5;
    private int alphaStep = 8;
    private int alphaShift = 0;
    private int color = ColorHelper.getColor(0, 255, 0, 255);

    private static final ReferenceSet<Block> lavaFilter = ReferenceSet.of(Blocks.LAVA);

    public void setDiskCache(boolean disk) {
        lavaColumnsCache.setDiskCache(disk, isEnabled());
    }

    @EventHandler
    public void onChunkData(final ChunkDataEvent event) {
        var level = mc.level;
        if (level == null || mc.levelRenderer.viewArea == null) return;
        var chunk = event.chunk();
        var chunkPos = chunk.getPos();
        if (getActualDimension() != Level.NETHER) return;
        if (lavaColumnsCache.get().isHighlighted(chunkPos.x, chunkPos.z, getActualDimension())) return;

        var maxHeight = new AtomicInteger(0);
        ChunkScanner.chunkScanBlockstatePredicate(chunk, lavaFilter, (c, state, relX, y, relZ) -> {
            var x = ChunkUtils.chunkCoordToCoord(c.getPos().x) + relX;
            var z = ChunkUtils.chunkCoordToCoord(c.getPos().z) + relZ;
            var maxY = level.getMaxBuildHeight();
            var fluid = state.getFluidState();
            if (!fluid.isEmpty() && !fluid.isSource()) {
                var columnHeight = 1;
                for (int yy = y + 1; yy <= maxY; yy++) {
                    var aboveState = chunk.getFluidState(x, yy, z);
                    if (aboveState.isEmpty() || aboveState.isSource()) break;
                    columnHeight++;
                }
                if (columnHeight >= maxHeight.get()) {
                    maxHeight.set(columnHeight);
                }
            }
            return false;
        }, 0);

        lavaColumnsCache.get().addHighlight(chunk.getPos().x, chunk.getPos().z, maxHeight.get());
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registry().register(
            DrawFeatureFactory.multiColorChunkHighlights(
                "LavaColumns",
                this::getLavaColumnHeightHighlights,
                this::colorFunction,
                250
            )
        );
        lavaColumnsCache.onEnable();
    }

    private Long2LongMap getLavaColumnHeightHighlights(ResourceKey<Level> dim) {
        return lavaColumnsCache.get().getCacheMap(dim);
    }

    private int colorFunction(long pos, long columnHeight) {
        if (columnHeight < minColumnHeight) return 0;
        int alpha = Mth.clamp(alphaShift + (((int) columnHeight) * alphaStep), 0, 255);
        return ColorHelper.getColorWithAlpha(color, alpha);
    }

    @Override
    public void onDisable() {
        lavaColumnsCache.onDisable();
        Globals.drawManager.registry().unregister("LavaColumns");
    }

    public void setMinColumnHeight(final int minHeight) {
        this.minColumnHeight = minHeight;
    }

    public void setAlphaStep(final int alphaStep) {
        this.alphaStep = alphaStep;
    }

    public void setRgbColor(final int color) {
        this.color = color;
    }

    public void setAlphaShift(final int alphaShift) {
        this.alphaShift = alphaShift;
    }
}
