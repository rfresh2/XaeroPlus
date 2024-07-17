package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.ChunkHighlightProvider;
import xaeroplus.feature.render.ColorHelper;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;

import static xaeroplus.feature.render.ColorHelper.getColor;

public class Highways extends Module {
    private int highwaysColor = getColor(0, 0, 255, 100);

    // Highway data sourced from: https://www.desmos.com/calculator/ogsleiq29o

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
    private static final int fiveK = ChunkUtils.posToChunkPos(5000);

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            new ChunkHighlightProvider(
                this::isHighwayChunk,
                this::getHighwayColor
            ));
    }

    @Override
    public void onDisable() {
        Globals.drawManager.unregister(this.getClass());
    }

    public boolean isHighwayChunk(int x, int z, ResourceKey<Level> dimension) {
        if (x == 0 || z == 0) return true; // cardinal directions
        var xAbs = Math.abs(x);
        var zAbs = Math.abs(z);
        if (xAbs == zAbs) return true; // diags
        if (dimension == Level.NETHER) {
            // ring roads
            if (ringRoads.contains(xAbs)) {
                if (z >= -xAbs && z <= xAbs) return true;
            }
            if (ringRoads.contains(zAbs)) {
                if (x >= -zAbs && x <= zAbs) return true;
            }

            // diamonds
            if (diamonds.contains(xAbs + zAbs)) return true;

            // grid
            if (xAbs < fiftyK && zAbs < fiftyK) {
                if (xAbs % fiveK == 0) return true;
                if (zAbs % fiveK == 0) return true;
            }
        }
        return false;
    }

    public int getHighwayColor() {
        return highwaysColor;
    }

    public void setRgbColor(final int color) {
        highwaysColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.highwaysColorAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        highwaysColor = ColorHelper.getColorWithAlpha(highwaysColor, (int) a);
    }

    private static IntOpenHashSet chunkSetFromPosList(int... pos) {
        final IntOpenHashSet set = new IntOpenHashSet(pos.length);
        for (int i = 0; i < pos.length; i++) {
            set.add(ChunkUtils.posToChunkPos(pos[i]));
        }
        return set;
    }
}
