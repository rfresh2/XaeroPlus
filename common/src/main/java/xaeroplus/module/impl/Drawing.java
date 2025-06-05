package xaeroplus.module.impl;

import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.render.Line;
import xaeroplus.feature.render.drawing.DrawingCache;
import xaeroplus.module.Module;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.DrawingMode;

import java.util.*;

public class Drawing extends Module {
    private final Map<ResourceKey<Level>, List<Line>> lines = new HashMap<>(Map.of(
        Level.OVERWORLD, new ArrayList<>(),
        Level.NETHER, new ArrayList<>(),
        Level.END, new ArrayList<>()
    ));
    public final DrawingCache drawingCache = new DrawingCache("XaeroPlusDrawing");
    private Line inProgressLine = null;
    private int savedColorAlpha = 255;
    private final DrawingColorCycler drawingColorCycler = new DrawingColorCycler();
    private final int inProgressColorAlpha = 80;

    public DrawingColorCycler getDrawingColorCycler() {
        return drawingColorCycler;
    }

    @Override
    public void onEnable() {
        drawingCache.onEnable();
        Globals.drawManager.registry().registerLineProvider(
            this.getClass().getName() + "-saved",
            this::getSavedLines,
            () -> drawingColorCycler.getColorInt(savedColorAlpha),
            () -> 0.5f,
            50
        );
        Globals.drawManager.registry().registerLineProvider(
            this.getClass().getName() + "-in-progress",
            this::getInProgressLines,
            () -> drawingColorCycler.getColorInt(inProgressColorAlpha),
            () -> 0.5f,
            1
        );
        Globals.drawManager.registry().registerDirectChunkColoredHighlightProvider(
            this.getClass().getName() + "-highlights",
            true,
            drawingCache::getCacheMap,
            () -> savedColorAlpha
        );
    }

    @EventHandler
    public void onTick(final ClientTickEvent.Post event) {
        drawingCache.handleTick();
    }

    @EventHandler
    public void onWorldChange(final XaeroWorldChangeEvent event) {
        drawingCache.handleWorldChange(event);
    }

    private List<Line> getSavedLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return this.lines.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    private List<Line> getInProgressLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        var l = inProgressLine;
        if (inProgressLine != null) {
            return List.of(l);
        } else {
            return Collections.emptyList();
        }
    }

    // call on server switch?
    public void resetLines() {
        for (var entry : lines.entrySet()) {
            entry.getValue().clear();
        }
    }

    public void addLine(final Line line) {
        if (line.length() == 0) return;
        var lines = this.lines.computeIfAbsent(Globals.getCurrentDimensionId(), k -> new ArrayList<>());
        lines.add(line);
    }

    public void addInfiniteLine(final Line line) {
        if (line.length() == 0) return;
        var lines = this.lines.computeIfAbsent(Globals.getCurrentDimensionId(), k -> new ArrayList<>());
        lines.add(line.extrapolateToWorldBorder());
    }

    public void addHighlight(int chunkX, int chunkZ, int color) {
        drawingCache.addHighlight(chunkX, chunkZ, color, Globals.getCurrentDimensionId());
    }

    public void addHighlight(int chunkX, int chunkZ) {
        addHighlight(chunkX, chunkZ, drawingColorCycler.getColor().getColor());
    }

    public void removeHighlight(final int chunkX, final int chunkZ) {
        drawingCache.removeHighlight(chunkX, chunkZ, Globals.getCurrentDimensionId());
    }

    public void setInProgressLine(final Line inProgressLine, final DrawingMode drawingMode) {
        switch (drawingMode) {
            case LINE_SEGMENT -> this.inProgressLine = inProgressLine;
            case INFINITE_LINE -> this.inProgressLine = inProgressLine.extrapolateToWorldBorder();
        }
    }

    public void clearInProgressLine() {
        inProgressLine = null;
    }

    public void clearLine(final int x, final int z) {
        var lines = this.lines.computeIfAbsent(Globals.getCurrentDimensionId(), k -> new ArrayList<>());
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

    public static final class DrawingColorCycler {
        private int index;

        public DrawingColorCycler() {
            index = 0;
        }

        public void setColor(ColorHelper.HighlightColor color) {
            this.index = color.ordinal();
        }

        public ColorHelper.HighlightColor getColor() {
            return ColorHelper.HighlightColor.fromIndex(index);
        }

        public int getColorInt(int alpha) {
            int c = ColorHelper.HighlightColor.fromIndex(index).getColor();
            return ColorHelper.getColorWithAlpha(c, alpha);
        }

        public void next() {
            index++;
            if (index >= ColorHelper.HighlightColor.VALUES.length) {
                index = 0;
            }
        }
    }
}
