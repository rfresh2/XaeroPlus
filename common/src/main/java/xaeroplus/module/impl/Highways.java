package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import static xaeroplus.util.ColorHelper.getColor;

public class Highways extends Module {
    private int highwaysColor = getColor(0, 0, 255, 100);
    private int width = 2;

    // Highway data sourced from: https://www.desmos.com/calculator/oexoz81fxy

    /**
     * Known Errors / Missing Data
     *
     * Lesser known highways like Old Spawn Road and Flower Trail (and many more)
     *
     * OW/End highways extend past actual road
     */

    private static final IntSet ringRoads = chunkSetFromPosList(
        200,
        500,
        1000,
        1500,
        2000,
        2500,
        5000,
        7500,
        10000,
        15000,
        20000,
        25000,
        50000,
        55000,
        62500,
        100000,
        125000,
        250000,
        500000,
        750000,
        1000000,
        1250000,
        1568852,
        1875000,
        2500000,
        3750000
    );

    private static final IntSet diamonds = chunkSetFromPosList(
        2500,
        5000,
        25000,
        50000,
        125000,
        250000,
        500000,
        3750000
    );

    private static final int fiftyK = ChunkUtils.posToChunkPos(50000);

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            this::getWindowedHighlightsSnapshot,
            this::getHighwayColor);
    }

    @Override
    public void onDisable() {
        Globals.drawManager.unregisterChunkHighlightProvider(this.getClass());
    }

    public boolean isHighwayChunk(int x, int z, ResourceKey<Level> dimension) {
        if (x == 0 || z == 0) return true; // cardinal directions
        int w = width / 2;
        if (w >= 1) {
            for (int i = 1; i <= w; i++) {
                if (x + i == 0 || x - i == 0) return true;
                if (z + i == 0 || z - i == 0) return true;
            }
        }
        var xAbs = Math.abs(x);
        var zAbs = Math.abs(z);
        if (xAbs == zAbs) return true; // diags
        if (w >= 1) {
            for (int i = 1; i <= w; i++) {
                if (xAbs - i == zAbs || xAbs + i == zAbs) return true;
            }
        }
        if (dimension == Level.NETHER) {
            // ring roads
            if (ringRoads.contains(xAbs)) {
                if (z >= -xAbs && z <= xAbs) return true;
            }
            if (w >= 1) {
                for (int i = 1; i <= w; i++) {
                    if (ringRoads.contains(xAbs - i)) {
                        if (z >= -xAbs + i && z <= xAbs - i) return true;
                    }
                    if (ringRoads.contains(xAbs + i)) {
                        if (z >= -xAbs - i && z <= xAbs + i) return true;
                    }
                }
            }
            if (ringRoads.contains(zAbs)) {
                if (x >= -zAbs && x <= zAbs) return true;
            }
            if (w >= 1) {
                for (int i = 1; i <= w; i++) {
                    if (ringRoads.contains(zAbs - i)) {
                        if (x >= -zAbs + i && x <= zAbs - i) return true;
                    }
                    if (ringRoads.contains(zAbs + i)) {
                        if (x >= -zAbs - i && x <= zAbs + i) return true;
                    }
                }
            }

            // diamonds
            if (diamonds.contains(xAbs + zAbs)) return true;
            if (w >= 1) {
                for (int i = 1; i <= w; i++) {
                    if (diamonds.contains(xAbs + zAbs - i)) return true;
                    if (diamonds.contains(xAbs + zAbs + i)) return true;
                }
            }

            // grid
            if (xAbs < fiftyK && zAbs < fiftyK) {
                if ((xAbs * 16) % 5000 == 0) return true;
                if ((zAbs * 16) % 5000 == 0) return true;
                if (w >= 1) {
                    for (int i = 1; i <= w; i++) {
                        if (((xAbs - i) * 16) % 5000 == 0) return true;
                        if (((xAbs + i) * 16) % 5000 == 0) return true;
                        if (((zAbs - i) * 16) % 5000 == 0) return true;
                        if (((zAbs + i) * 16) % 5000 == 0) return true;
                    }
                }
            }
        }
        return false;
    }

    public LongList getWindowedHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        int minChunkX = ChunkUtils.regionCoordToChunkCoord(windowRegionX - windowRegionSize);
        int maxChunkX = ChunkUtils.regionCoordToChunkCoord(windowRegionX + windowRegionSize);
        int minChunkZ = ChunkUtils.regionCoordToChunkCoord(windowRegionZ - windowRegionSize);
        int maxChunkZ = ChunkUtils.regionCoordToChunkCoord(windowRegionZ + windowRegionSize);
        LongList chunks = new LongArrayList(8);
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (isHighwayChunk(x, z, dimension)) {
                    chunks.add(ChunkUtils.chunkPosToLong(x, z));
                }
            }
        }
        return chunks;
    }

    public int getHighwayColor() {
        return highwaysColor;
    }

    public void setRgbColor(final int color) {
        highwaysColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.highwaysColorAlphaSetting.getAsInt());
    }

    public void setAlpha(final double a) {
        highwaysColor = ColorHelper.getColorWithAlpha(highwaysColor, (int) a);
    }

    public void setWidth(final Settings.HighwayWidth w) {
        width = w.getWidth();
    }

    private static IntOpenHashSet chunkSetFromPosList(int... pos) {
        final IntOpenHashSet set = new IntOpenHashSet(pos.length);
        for (int i = 0; i < pos.length; i++) {
            set.add(ChunkUtils.posToChunkPos(pos[i]));
        }
        return set;
    }
}
