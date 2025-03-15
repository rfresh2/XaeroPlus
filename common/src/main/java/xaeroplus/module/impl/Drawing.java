package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.Line;
import xaeroplus.module.Module;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Drawing extends Module {
    private final Map<ResourceKey<Level>, List<Line>> lines = new Reference2ObjectOpenHashMap<>(Map.of(
        Level.OVERWORLD, new ArrayList<>(),
        Level.NETHER, new ArrayList<>(),
        Level.END, new ArrayList<>()
    ));
    private Line inProgressLine = null;
    private int savedColor = ColorHelper.getColor(255, 0, 0, 255);
    private int inProgressColor = ColorHelper.getColor(255, 0, 0, 80);

    @Override
    public void onEnable() {
        Globals.drawManager.registry().registerLineProvider(
            this.getClass().getName() + "-saved",
            this::getSavedLines,
            () -> savedColor,
            () -> 0.5f,
            50
        );
        Globals.drawManager.registry().registerLineProvider(
            this.getClass().getName() + "-in-progress",
            this::getInProgressLines,
            () -> inProgressColor,
            () -> 0.5f,
            1
        );
    }

    private List<Line> getSavedLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        var lines = this.lines.get(dimension);
        if (lines == null) {
            return Collections.emptyList();
        }
        return lines;
    }

    private List<Line> getInProgressLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        var l = inProgressLine;
        if (inProgressLine != null) {
            return List.of(l);
        } else {
            return Collections.emptyList();
        }
    }

    public void addLine(final Line line) {
        var lines = this.lines.get(Globals.getCurrentDimensionId());
        if (lines == null) return;
        if (line.length() == 0) return;
        lines.add(line);
    }

    public void addInfiniteLine(final Line line) {
        var lines = this.lines.get(Globals.getCurrentDimensionId());
        if (lines == null) return;
        if (line.length() == 0) return;
        // extrapolate the line segment to square -30m,-30m and 30m,30m in 500k increments
        // y = mx + b
        int dx = line.x2() - line.x1();
        if (dx == 0) {
            for (int x = -30_000_000; x <= 30_000_000; x += 500_000) {
                lines.add(new Line(x, line.z1(), x, line.z2()));
            }
            return;
        }
        int dz = line.z2() - line.z1();
        if (dz == 0) {
            for (int z = -30_000_000; z <= 30_000_000; z += 500_000) {
                lines.add(new Line(line.x1(), z, line.x2(), z));
            }
            return;
        }
        double slope = (double) dz / dx;
        double intercept = line.z1() - slope * line.x1();

        // now walk along the line in 500k length increments, starting at x1, z1, until we reach +-30_000_000 in either x or z
        // we can size the walk length to 500k using our slope
        // i.e. if slope is 2, then we can walk 250k x to get 500k z
        int x1 = line.x1();
        int z1 = line.z1();
        int x = x1;
        int z = z1;
        boolean positiveSlope = Math.abs(slope) > 1;
        // if positive slope, we are increasing z faster than x
        // and therefore need to increment z by 500k
        while (x >= -30_000_000 && x <= 30_000_000 && z >= -30_000_000 && z <= 30_000_000) {
            int prevX = x;
            int prevZ = z;
            if (positiveSlope) {
                z += 500_000;
                x = (int) ((z - intercept) / slope);
            } else {
                x += 500_000;
                z = (int) (slope * x + intercept);
            }
            lines.add(new Line(prevX, prevZ, x, z));
        }
        // now walk the other direction
        x = x1;
        z = z1;
        while (x >= -30_000_000 && x <= 30_000_000 && z >= -30_000_000 && z <= 30_000_000) {
            int prevX = x;
            int prevZ = z;
            if (positiveSlope) {
                z -= 500_000;
                x = (int) ((z - intercept) / slope);
            } else {
                x -= 500_000;
                z = (int) (slope * x + intercept);
            }
            lines.add(new Line(prevX, prevZ, x, z));
        }
    }

    public void setInProgressLine(final Line inProgressLine) {
        this.inProgressLine = inProgressLine;
    }

    public void clearInProgressLine() {
        inProgressLine = null;
    }

    public void clearLine(final int x, final int z) {
        var lines = this.lines.get(Globals.getCurrentDimensionId());
        if (lines == null) return;
        int maxX = x + 16;
        int maxZ = z + 16;
        Line sqLine1 = new Line(x, z, maxX, z);
        Line sqLine2 = new Line(x, z, x, maxZ);
        Line sqLine3 = new Line(maxX, z, maxX, maxZ);
        Line sqLine4 = new Line(x, maxZ, maxX, maxZ);
        // find lines which intersect with square (x, z, maxX, maxZ)
        lines.removeIf(line -> {
            var lx1 = line.x1();
            var lz1 = line.z1();
            var lx2 = line.x2();
            var lz2 = line.z2();
            if (lx1 < x && lx2 < x) return false;
            if (lz1 < z && lz2 < z) return false;
            if (lx1 > maxX && lx2 > maxX) return false;
            if (lz1 > maxZ && lz2 > maxZ) return false;
            return lineIntersects(line, sqLine1) || lineIntersects(line, sqLine2) || lineIntersects(line, sqLine3) || lineIntersects(line, sqLine4);
        });
    }

    private boolean lineIntersects(Line line1, Line line2) {
        var x1 = line1.x1();
        var z1 = line1.z1();
        var x2 = line1.x2();
        var z2 = line1.z2();
        var x3 = line2.x1();
        var z3 = line2.z1();
        var x4 = line2.x2();
        var z4 = line2.z2();
        var d = (z4 - z3) * (x2 - x1) - (x4 - x3) * (z2 - z1);
        if (d == 0) return false;
        var uA = ((x4 - x3) * (z1 - z3) - (z4 - z3) * (x1 - x3)) / d;
        var uB = ((x2 - x1) * (z1 - z3) - (z2 - z1) * (x1 - x3)) / d;
        return uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1;
    }
}
