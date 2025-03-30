package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.Line;
import xaeroplus.module.Module;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.DrawingMode;

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
        lines.add(extrapolateToWorldBorder(line));
    }

    private Line extrapolateToWorldBorder(Line line) {
        int wb = 30_000_000;
        // extrapolate the line and find its intersections with the world border rect
        int dx = line.x2() - line.x1();
        if (dx == 0) { // vertical line
            return new Line(line.x1(), -wb, line.x2(), wb);
        }
        int dz = line.z2() - line.z1();
        if (dz == 0) { // horizontal line
            return new Line(-wb, line.z1(), wb, line.z2());
        }
        double slope = (double) dz / dx;
        double intercept = line.z1() - slope * line.x1();
        double x1 = -wb;
        double z1 = slope * x1 + intercept;
        if (z1 < -wb) {
            z1 = -wb;
            x1 = (z1 - intercept) / slope;
        } else if (z1 > wb) {
            z1 = wb;
            x1 = (z1 - intercept) / slope;
        }
        double x2 = wb;
        double z2 = slope * x2 + intercept;
        if (z2 < -wb) {
            z2 = -wb;
            x2 = (z2 - intercept) / slope;
        } else if (z2 > wb) {
            z2 = wb;
            x2 = (z2 - intercept) / slope;
        }
        return new Line((int) Math.round(x1), (int) Math.round(z1), (int) Math.round(x2), (int) Math.round(z2));
    }

    public void setInProgressLine(final Line inProgressLine, final DrawingMode drawingMode) {
        switch (drawingMode) {
            case LINE_SEGMENT -> this.inProgressLine = inProgressLine;
            case INFINITE_LINE -> this.inProgressLine = extrapolateToWorldBorder(inProgressLine);
        }
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
            if (line.x1() < x && line.x2() < x) return false;
            if (line.z1() < z && line.z2() < z) return false;
            if (line.x1() > maxX && line.x2() > maxX) return false;
            if (line.z1() > maxZ && line.z2() > maxZ) return false;
            return linesIntersect(line, sqLine1)
                || linesIntersect(line, sqLine2)
                || linesIntersect(line, sqLine3)
                || linesIntersect(line, sqLine4);
        });
    }

    private boolean linesIntersect(Line line1, Line line2) {
        double bx = line1.x2() - line1.x1();
        double bz = line1.z2() - line1.z1();
        double dx = line2.x2() - line2.x1();
        double dz = line2.z2() - line2.z1();
        double bDotDPerp = bx * dz - bz * dx;
        if (Math.round(bDotDPerp) == 0) return false;
        int cx = line2.x1() - line1.x1();
        int cz = line2.z1() - line1.z1();
        double t = (cx * dz - cz * dx) / bDotDPerp;
        if (t < 0 || t > 1) return false;
        double u = (cx * bz - cz * bx) / bDotDPerp;
        return u >= 0 && u <= 1;
    }
}
