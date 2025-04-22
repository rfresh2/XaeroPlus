package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.ints.IntList;
import kaptainwutax.mathutils.util.Mth;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.Line;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.List;

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

    private final List<Line> OVERWORLD_END_LINES = generateHighwayLines(Level.OVERWORLD);
    private final List<Line> NETHER_LINES = generateHighwayLines(Level.NETHER);

    private static final IntList ringRoads = IntList.of(
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
        75000,
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

    private static final IntList diamonds = IntList.of(
        2500,
        5000,
        25000,
        50000,
        125000,
        250000,
        500000,
        3750000
    );

    @Override
    public void onEnable() {
        Globals.drawManager.registry().registerLineProvider(
            this.getClass().getName(),
            this::getHighwayLines,
            this::getHighwayColor,
            this::getLineWidth,
            5000
        );
    }

    private List<Line> getHighwayLines(int windowRegionX, int windowRegionZ, int windowSize, ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD || dimension == Level.END) {
            return OVERWORLD_END_LINES;
        } else if (dimension == Level.NETHER) {
            return NETHER_LINES;
        } else {
            return new ArrayList<>();
        }
    }

    private List<Line> generateHighwayLines(ResourceKey<Level> dimension) {
        var lines = new ArrayList<Line>(500);
        // if a line is too long we will start hitting floating point precision errors in opengl
        // as the lines are translated to map and screen space
        // so we break these up into smaller lines
        int stride = 500_000;

        // cardinals
        for (int i = -30_000_000; i < 30_000_000; i += stride) {
            lines.add(new Line(i, 0, i + stride, 0));
            lines.add(new Line(0, i, 0, i + stride));
        }

        // diagonals
        for (int i = -30_000_000; i < 30_000_000; i += stride) {
            lines.add(new Line(i, i, i + stride, i + stride));
            lines.add(new Line(-i, i, -i - stride, i + stride));
        }

        if (dimension == Level.NETHER) {
            // ring roads
            for (int ringRoad : ringRoads) {
                int i = -ringRoad;
                while (i < ringRoad) {
                    lines.add(new Line(i, -ringRoad, Mth.min(i + stride, ringRoad), -ringRoad));
                    lines.add(new Line(i, ringRoad, Mth.min(i + stride, ringRoad), ringRoad));
                    lines.add(new Line(-ringRoad, i, -ringRoad, Mth.min(i + stride, ringRoad)));
                    lines.add(new Line(ringRoad, i, ringRoad, Mth.min(i + stride, ringRoad)));
                    i += stride;
                }
            }

            // diamonds
            for (int diamond : diamonds) {
                lines.add(new Line(diamond, 0, 0, diamond));
                lines.add(new Line(0, -diamond, diamond, 0));
                lines.add(new Line(0, -diamond, -diamond, 0));
                lines.add(new Line(-diamond, 0, 0, diamond));
            }

            // 50k grid - vertical and horizontal line every 5k blocks
            for (int i = -50000; i < 50000; i += 5000) {
                if (i == 0) continue;
                lines.add(new Line(i, -50000, i, 50000));
                lines.add(new Line(-50000, i, 50000, i));
            }

            // 50k extension roads, extends 50k to 125k
            // horizontal
            lines.add(new Line(-125000, -50000, -50000, -50000));
            lines.add(new Line(-125000, 50000, -50000, 50000));
            lines.add(new Line(125000, -50000, 50000, -50000));
            lines.add(new Line(125000, 50000, 50000, 50000));
            // vertical
            lines.add(new Line(-50000, -125000, -50000, -50000));
            lines.add(new Line(-50000, 125000, -50000, 50000));
            lines.add(new Line(50000, -125000, 50000, -50000));
            lines.add(new Line(50000, 125000, 50000, 50000));
        }
        return lines;
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregisterLineProvider(this.getClass().getName());
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

    private float getLineWidth() {
        return width;
    }
}
