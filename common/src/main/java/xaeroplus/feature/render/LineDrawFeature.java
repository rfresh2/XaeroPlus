package xaeroplus.feature.render;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import xaeroplus.Globals;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static xaeroplus.util.GuiMapHelper.*;

public class LineDrawFeature {
    private final AsyncLoadingCache<Long, List<Line>> lineRenderCache;
    private final LineProvider lineProvider;

    public LineDrawFeature(LineProvider lineProvider, int refreshIntervalMs) {
        this.lineProvider = lineProvider;
        this.lineRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(refreshIntervalMs, TimeUnit.MILLISECONDS)
            .executor(ModuleManager.getModule(TickTaskExecutor.class))
            .buildAsync(k -> loadLinesInWindow());
    }

    private List<Line> loadLinesInWindow() {
        final int windowX, windowZ, windowSize;
        var guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            var guiMap = guiMapOptional.get();
            windowX = getGuiMapCenterRegionX(guiMap);
            windowZ = getGuiMapCenterRegionZ(guiMap);
            windowSize = getGuiMapRegionSize(guiMap);
        } else {
            windowX = ChunkUtils.getPlayerRegionX();
            windowZ = ChunkUtils.getPlayerRegionZ();
            windowSize = Math.max(3, Globals.minimapScaleMultiplier);
        }
        return preProcessLines(lineProvider.lineSupplier().getLines(windowX, windowZ, windowSize, Globals.getCurrentDimensionId()));
    }

    private List<Line> preProcessLines(List<Line> lines) {
        if (lines.isEmpty()) return lines;
        List<Line> out = new ArrayList<>(lines);
        for (int i = 0; i < out.size(); i++) {
            Line line = out.get(i);
            List<Line> newLines = ensureLength(line);
            if (!newLines.isEmpty()) {
                out.remove(i);
                out.addAll(i, newLines);
                i += newLines.size() - 1;
            }
        }
        out.replaceAll(this::ensureOrientation);
        return out;
    }

    private Line ensureOrientation(Line line) {
        // z1 must always be less than or equal to z2
        if (line.z1() > line.z2()) {
            return new Line(line.x2(), line.z2(), line.x1(), line.z1());
        } else {
            return line;
        }
    }

    public static final int MAX_LINE_LENGTH = 500_000;

    // ensure no line is longer than MAX_LINE_LENGTH
    // if a line is longer than MAX_LINE_LENGTH, split it into multiple lines each with a maximum length of MAX_LINE_LENGTH
    // else return an empty list indicating no change is needed
    // this is needed as otherwise we start hitting floating point precision issues as the line is rendered
    private List<Line> ensureLength(Line line) {
        double len = line.length();
        if (len <= MAX_LINE_LENGTH) {
            return Collections.emptyList();
        }
        List<Line> lines = new ArrayList<>((int) (len / MAX_LINE_LENGTH) + 1);
        int dx = line.x2() - line.x1();
        int dz = line.z2() - line.z1();
        if (dx == 0) { // vertical line
            for (int z = Math.min(line.z1(), line.z2()); z < Math.max(line.z1(), line.z2()); z += MAX_LINE_LENGTH) {
                lines.add(new Line(line.x1(), z, line.x2(), Math.min(z + MAX_LINE_LENGTH, line.z2())));
            }
        } else if (dz == 0) { // horizontal line
            for (int x = Math.min(line.x1(), line.x2()); x < Math.max(line.x1(), line.x2()); x += MAX_LINE_LENGTH) {
                lines.add(new Line(x, line.z1(), Math.min(x + MAX_LINE_LENGTH, line.x2()), line.z2()));
            }
        } else {
            double slope = (double) dz / dx;
            double intercept = line.z1() - slope * line.x1();
            // if positive slope, we are increasing z faster than x
            // and therefore need to increment z by 500k
            boolean positiveSlope = Math.abs(slope) > 1;
            if (positiveSlope) {
                for (double z = Math.min(line.z1(), line.z2()); z < Math.max(line.z1(), line.z2()); z += MAX_LINE_LENGTH) {
                    double x = (z - intercept) / slope;
                    lines.add(new Line(
                        (int) Math.round((z - MAX_LINE_LENGTH - intercept) / slope),
                        (int) Math.round(z - MAX_LINE_LENGTH),
                        (int) Math.round(x),
                        (int) Math.round(z)
                    ));
                }
            } else {
                for (double x = Math.min(line.x1(), line.x2()); x < Math.max(line.x1(), line.x2()); x += MAX_LINE_LENGTH) {
                    double z = slope * x + intercept;
                    lines.add(new Line(
                        (int) Math.round(x - MAX_LINE_LENGTH),
                        (int) Math.round(slope * (x - MAX_LINE_LENGTH) + intercept),
                        (int) Math.round(x),
                        (int) Math.round(z)
                    ));
                }
            }
        }
        return lines;
    }

    public int colorInt() {
        return lineProvider.colorSupplier().getAsInt();
    }

    public float lineWidth() {
        return lineProvider.lineWidthSupplier().getFloat();
    }

    public void invalidateCache() {
        lineRenderCache.synchronous().invalidateAll();
    }

    public List<Line> getLines() {
        return lineRenderCache.get(0L).getNow(Collections.emptyList());
    }
}
