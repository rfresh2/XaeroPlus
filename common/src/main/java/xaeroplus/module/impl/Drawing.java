package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.drawing.DrawingCache;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.feature.render.ellipse.Ellipse;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.Module;
import xaeroplus.util.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class Drawing extends Module {
    public final DrawingCache drawingCache = new DrawingCache("XaeroPlusDrawing");
    private Line inProgressLine = null;
    private Ellipse inProgressEllipse = null;
    private Color drawingColor = new Color(255, 0, 0, 200);
    private final Deque<DrawingOperation> operationStack = new LinkedBlockingDeque<>();
    private DrawingOperationCollector operationCollector = null;
    private final int inProgressColorAlpha = 80;

    public Color getDrawingColor() {
        return drawingColor;
    }

    public void setDrawingColor(final Color drawingColor) {
        this.drawingColor = Objects.requireNonNull(drawingColor);
    }

    // todo: wire in bulk selection delete undo?

    public void startOperation(ResourceKey<Level> dimension, boolean erase) {
        operationCollector = new DrawingOperationCollector(dimension, erase);
    }

    public void endOperation() {
        if (operationCollector != null) {
            var op = operationCollector.collect();
            if (op != null) {
                operationStack.push(op);
            }
            operationCollector = null;
        }
    }

    public void undoLastOperation() {
        if (!operationStack.isEmpty()) {
            var op = operationStack.pop();
            op.revert(this);
        }
    }

    @Override
    public void onEnable() {
        drawingCache.onEnable();
        Globals.drawManager.registry().register(
            DrawFeatureFactory.multiColorLines(
                "Drawing-lines-saved",
                this::getSavedLines,
                (line, v) -> v,
                () -> 0.5f,
                50
            )
        );
        Globals.drawManager.registry().register(
            DrawFeatureFactory.lines(
                "Drawing-lines-in-progress",
                this::getInProgressLines,
                () -> ColorHelper.getColorWithAlpha(drawingColor.getInt(), inProgressColorAlpha),
                () -> 0.5f,
                1
            )
        );
        Globals.drawManager.registry().register(
            DrawFeatureFactory.multiColorEllipses(
                "Drawing-ellipses-saved",
                this::getSavedEllipses,
                (ellipse, color) -> color,
                () -> 0.25f,
                50
            )
        );
        Globals.drawManager.registry().register(
            DrawFeatureFactory.ellipses(
                "Drawing-ellipses-in-progress",
                this::getInProgressEllipses,
                () -> drawingColor.getInt(),
                () -> 0.25f,
                1
            )
        );
        Globals.drawManager.registry().register(
            DrawFeatureFactory.multiColorChunkHighlights(
                "Drawing-highlights",
                drawingCache::getHighlights,
                (pos, t) -> (int) t,
                50
            )
        );
        Globals.drawManager.registry().register(
            DrawFeatureFactory.text(
                "Drawing-text",
                this::getTexts
            )
        );
        operationStack.clear();
    }

    private Long2ObjectMap<Text> getTexts(int windowRegionX, int windowRegionZ, int windowSize, ResourceKey<Level> dim) {
        return drawingCache.getTexts(dim);
    }

    @EventHandler
    public void onTick(final ClientTickEvent.Post event) {
        drawingCache.handleTick();
        if (GuiMapHelper.getGuiMap().isEmpty()) {
            operationStack.clear();
            operationCollector = null;
        }
    }

    @EventHandler
    public void onWorldChange(final XaeroWorldChangeEvent event) {
        if (!mc.isRunning()) return;
        drawingCache.handleWorldChange(event);
    }

    public CompletableFuture<Void> shutdown() {
        try {
            return drawingCache.onShutdown();
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Failed to close drawing cache", e);
            return CompletableFuture.completedFuture(null);
        }
    }

    private Object2IntMap<Line> getSavedLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return drawingCache.getLines(dimension);
    }

    private Object2IntMap<Ellipse> getSavedEllipses(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return drawingCache.getEllipses(dimension);
    }

    public Line getInProgressLine() {
        return inProgressLine;
    }

    private List<Line> getInProgressLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        var l = inProgressLine;
        if (inProgressLine != null) {
            return List.of(l);
        } else {
            return Collections.emptyList();
        }
    }

    private List<Ellipse> getInProgressEllipses(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        var ellipse = inProgressEllipse;
        return ellipse == null ? Collections.emptyList() : List.of(ellipse);
    }

    public void addLine(final Line line, int color) {
        if (line.length() < 2) return;
        var dimension = Globals.getCurrentDimensionId();
        var savedLines = drawingCache.getLines(dimension);
        var previousColor = savedLines.containsKey(line) ? savedLines.getInt(line) : null;
        drawingCache.addLine(line, color, dimension);
        if (operationCollector != null) {
            operationCollector.addLine(line, previousColor);
        }
    }

    public void addLine(final Line line) {
        addLine(line, drawingColor.getInt());
    }

    public void addInfiniteLine(final Line line, int color) {
        if (line.length() < 2) return;
        var infLine = line.extrapolateToWorldBorder();
        var dimension = Globals.getCurrentDimensionId();
        var savedLines = drawingCache.getLines(dimension);
        var previousColor = savedLines.containsKey(infLine) ? savedLines.getInt(infLine) : null;
        drawingCache.addLine(infLine, color, dimension);
        if (operationCollector != null) {
            operationCollector.addLine(infLine, previousColor);
        }
    }

    public void addInfiniteLine(final Line line) {
        addInfiniteLine(line, drawingColor.getInt());
    }

    public void addEllipse(final Ellipse ellipse, final int color) {
        var dimension = Globals.getCurrentDimensionId();
        var savedEllipses = drawingCache.getEllipses(dimension);
        var previousColor = savedEllipses.containsKey(ellipse) ? savedEllipses.getInt(ellipse) : null;
        drawingCache.addEllipse(ellipse, color, dimension);
        if (operationCollector != null) {
            operationCollector.addEllipse(ellipse, previousColor);
        }
    }

    public void addEllipse(final Ellipse ellipse) {
        addEllipse(ellipse, drawingColor.getInt());
    }

    public void addHighlight(int chunkX, int chunkZ, int color) {
        var dimension = Globals.getCurrentDimensionId();
        var key = ChunkUtils.chunkPosToLong(chunkX, chunkZ);
        var savedHighlights = drawingCache.getHighlights(dimension);
        Long previousColor = savedHighlights.containsKey(key) ? savedHighlights.get(key) : null;
        drawingCache.addHighlight(chunkX, chunkZ, color, dimension);
        if (operationCollector != null) {
            operationCollector.addHighlight(key, previousColor);
        }
    }

    public void addHighlight(int chunkX, int chunkZ) {
        addHighlight(chunkX, chunkZ, drawingColor.getInt());
    }

    public void removeHighlight(final int chunkX, final int chunkZ) {
        var dimension = Globals.getCurrentDimensionId();
        var key = ChunkUtils.chunkPosToLong(chunkX, chunkZ);
        var highlights = drawingCache.getHighlights(dimension);
        if (!highlights.containsKey(key)) return;
        var color = highlights.get(key);
        drawingCache.removeHighlight(chunkX, chunkZ, dimension);
        if (operationCollector != null) {
            operationCollector.addErasedHighlight(key, color);
        }
    }

    public void removeHighlights(final LongCollection toRemove) {
        var matched = new Long2LongOpenHashMap();
        var it = toRemove.longIterator();
        var existingCache = drawingCache.getHighlights(Globals.getCurrentDimensionId());
        while (it.hasNext()) {
            long chunkLong = it.nextLong();
            if (existingCache.containsKey(chunkLong)) {
                matched.put(chunkLong, existingCache.get(chunkLong));
            }
        }
        drawingCache.removeHighlights(matched.keySet(), Globals.getCurrentDimensionId());
        if (operationCollector != null) {
            operationCollector.addErasedHighlights(matched);
        }
    }

    public void addText(final Text text) {
        if (text.value().isBlank()) return;
        var dimension = Globals.getCurrentDimensionId();
        var key = ChunkUtils.chunkPosToLong(text.x(), text.z());
        var savedTexts = drawingCache.getTexts(dimension);
        var previousText = savedTexts.get(key);
        drawingCache.addText(text, dimension);
        if (operationCollector != null) {
            operationCollector.addText(text, previousText);
        }
    }

    public void removeText(int x, int z, float viewScale) {
        // todo: search within certain range bound
        var texts = drawingCache.getTexts(Globals.getCurrentDimensionId());
        List<Text> toRemove =  new ArrayList<>();
        for (var text : texts.values()) {
            int textX = text.x();
            int textZ = text.z();
            String value = text.value();
            int valueFontWidth = mc.font.width(value);
            int valueHeight = mc.font.lineHeight;
            float textScale = text.scale() * 2.0f * Mth.clamp(
              1f / viewScale,
              1f,
              1000f
            );
            int textMinX = Mth.floor(textX - ((valueFontWidth / 2.0f) * textScale));
            int textMaxX = Mth.floor(textX + ((valueFontWidth / 2.0f) * textScale));
            int textMinZ = Mth.floor(textZ - ((valueHeight / 2.0f) * textScale));
            int textMaxZ = Mth.floor(textZ + ((valueHeight / 2.0f) * textScale));
            if (x >= textMinX && x <= textMaxX && z >= textMinZ && z <= textMaxZ) {
                toRemove.add(text);
            }
        }
        for (Text text : toRemove) {
            drawingCache.removeText(text.x(), text.z(), Globals.getCurrentDimensionId());
            if (operationCollector != null) {
                operationCollector.addErasedText(text);
            }
        }
    }

    public void setInProgressLine(final Line inProgressLine, final DrawingMode drawingMode) {
        switch (drawingMode) {
            case LINE_SEGMENT, MEASUREMENT -> this.inProgressLine = inProgressLine;
            case INFINITE_LINE -> this.inProgressLine = inProgressLine.extrapolateToWorldBorder();
        }
    }

    public void removeInProgressLine() {
        inProgressLine = null;
    }

    public void setInProgressEllipse(final Ellipse ellipse) {
        inProgressEllipse = ellipse;
    }

    public void removeInProgressEllipse() {
        inProgressEllipse = null;
    }

    public Ellipse ellipseFromCenterAndRadii(final int centerX, final int centerZ, final int radiusPointX, final int radiusPointZ) {
        var radiusX = Math.abs(radiusPointX - centerX);
        var radiusZ = Math.abs(radiusPointZ - centerZ);
        if (radiusX == 0 || radiusZ == 0) return null;
        return new Ellipse(centerX, centerZ, radiusX, radiusZ);
    }

    public Ellipse snapEllipse(
        final int centerX,
        final int centerZ,
        final int radiusPointX,
        final int radiusPointZ,
        final double scale
    ) {
        var ellipse = ellipseFromCenterAndRadii(centerX, centerZ, radiusPointX, radiusPointZ);
        if (ellipse == null) return null;
        var threshold = getSnapThreshold(scale);
        var dragLength = Mth.floor(Math.sqrt(
            Math.pow(radiusPointX - centerX, 2)
                + Math.pow(radiusPointZ - centerZ, 2)
        ));
        if (dragLength <= threshold) return ellipse;
        var radiusDelta = Math.abs(ellipse.radiusX() - ellipse.radiusZ());
        if (radiusDelta != 0 && radiusDelta < threshold) {
            var radius = Math.min(ellipse.radiusX(), ellipse.radiusZ());
            return new Ellipse(centerX, centerZ, radius, radius);
        }
        return ellipse;
    }

    public void removeLine(final int x, final int z) {
        Object2IntMap<Line> lines = drawingCache.getLines(Globals.getCurrentDimensionId());
        int maxX = x + 16;
        int maxZ = z + 16;
        Line sqLine1 = new Line(x, z, maxX, z);
        Line sqLine2 = new Line(x, z, x, maxZ);
        Line sqLine3 = new Line(maxX, z, maxX, maxZ);
        Line sqLine4 = new Line(x, maxZ, maxX, maxZ);
        var toRemove = new Object2IntOpenHashMap<Line>();
        // find lines which intersect with square (x, z, maxX, maxZ)
        var it = Object2IntMaps.fastIterator(lines);
        while (it.hasNext()) {
            var entry = it.next();
            Line line = entry.getKey();
            if (line.x1() < x && line.x2() < x) continue;
            if (line.z1() < z && line.z2() < z) continue;
            if (line.x1() > maxX && line.x2() > maxX) continue;
            if (line.z1() > maxZ && line.z2() > maxZ) continue;
            if (linesIntersect(line, sqLine1)
                || linesIntersect(line, sqLine2)
                || linesIntersect(line, sqLine3)
                || linesIntersect(line, sqLine4)) {
                toRemove.put(line, entry.getIntValue());
            }
        }
        for (var entry : toRemove.object2IntEntrySet()) {
            var line = entry.getKey();
            drawingCache.removeLine(line, Globals.getCurrentDimensionId());
            if (operationCollector != null) {
                operationCollector.addErasedLine(line, entry.getIntValue());
            }
        }
    }

    public void removeEllipse(final int x, final int z) {
        var maxX = x + 16;
        var maxZ = z + 16;
        var ellipses = drawingCache.getEllipses(Globals.getCurrentDimensionId());
        var toRemove = new Object2IntOpenHashMap<Ellipse>();
        for (var entry : ellipses.object2IntEntrySet()) {
            var ellipse = entry.getKey();
            if (ellipseOutlineIntersectsRectangle(ellipse, x, maxX, z, maxZ)) {
                toRemove.put(ellipse, entry.getIntValue());
            }
        }
        for (var entry : toRemove.object2IntEntrySet()) {
            var ellipse = entry.getKey();
            drawingCache.removeEllipse(ellipse, Globals.getCurrentDimensionId());
            if (operationCollector != null) {
                operationCollector.addErasedEllipse(ellipse, entry.getIntValue());
            }
        }
    }

    private boolean ellipseOutlineIntersectsRectangle(
        final Ellipse ellipse,
        final int minX,
        final int maxX,
        final int minZ,
        final int maxZ
    ) {
        if (!ellipse.intersects(minX, maxX, minZ, maxZ)) return false;
        var closestX = Mth.clamp(ellipse.centerX(), minX, maxX);
        var closestZ = Mth.clamp(ellipse.centerZ(), minZ, maxZ);
        var furthestX = Math.max(Math.abs((long) minX - ellipse.centerX()), Math.abs((long) maxX - ellipse.centerX()));
        var furthestZ = Math.max(Math.abs((long) minZ - ellipse.centerZ()), Math.abs((long) maxZ - ellipse.centerZ()));
        var minNormalizedDistance = normalizedEllipseDistance(ellipse, closestX - ellipse.centerX(), closestZ - ellipse.centerZ());
        var maxNormalizedDistance = normalizedEllipseDistance(ellipse, furthestX, furthestZ);
        return minNormalizedDistance <= 1.0 && maxNormalizedDistance >= 1.0;
    }

    private double normalizedEllipseDistance(final Ellipse ellipse, final long deltaX, final long deltaZ) {
        var normalizedX = (double) deltaX / ellipse.radiusX();
        var normalizedZ = (double) deltaZ / ellipse.radiusZ();
        return normalizedX * normalizedX + normalizedZ * normalizedZ;
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

    private static final int SNAP_THRESHOLD = 10;
    public Line snap(int x1, int z1, int x2, int z2, double scale) {
        int threshold = getSnapThreshold(scale);
        int len = Mth.floor(Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2)));
        if (len <= threshold) {
            return new Line(x1, z1, x2, z2);
        }
        int xDelta = Math.abs(x2 - x1);
        int zDelta = Math.abs(z2 - z1);
        // cardinals
        if (xDelta < threshold) {
            return new Line(x1, z1, x1, z2);
        }
        if (zDelta < threshold) {
            return new Line(x1, z1, x2, z1);
        }
        // diagonals
        int dDelta = Math.abs(xDelta - zDelta);
        if (dDelta != 0 && dDelta < threshold) {
            if (zDelta < xDelta) {
                int xSignum = x2 - x1 >= 0 ? 1 : -1;
                return new Line(x1, z1, x1 + zDelta * xSignum, z2);
            } else if (xDelta < zDelta) {
                int zSignum = z2 - z1 >= 0 ? 1 : -1;
                return new Line(x1, z1, x2, z1 + xDelta * zSignum);
            }
        }

        return new Line(x1, z1, x2, z2);
    }

    private int getSnapThreshold(final double scale) {
        var scalar = 1.0 / scale;
        return Mth.clamp(Mth.floor(SNAP_THRESHOLD * scalar), 10, 1000);
    }

    public void clearAll() {
        drawingCache.getAllHighlightCaches().forEach(c -> {
            c.removeAllHighlights();
        });
        drawingCache.getAllLinesCaches().forEach(c -> {
            c.removeAllLines();
        });
        drawingCache.getAllEllipseCaches().forEach(c -> {
            c.removeAllEllipses();
        });
        drawingCache.getAllTextsCaches().forEach(c -> {
            c.removeAllTexts();
        });
        operationCollector = null;
        operationStack.clear();
    }

    public interface DrawingOperation {
        void revert(Drawing drawing);
    }

    public record HighlightDrawingOperation(Long2LongMap replacedHighlights, LongSet addedHighlights, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            for (var chunkLong : addedHighlights) {
                var chunkX = ChunkUtils.longToChunkX(chunkLong);
                var chunkZ = ChunkUtils.longToChunkZ(chunkLong);
                drawing.drawingCache.removeHighlight(chunkX, chunkZ, dimension);
            }
            for (var entry : replacedHighlights.long2LongEntrySet()) {
                var chunkX = ChunkUtils.longToChunkX(entry.getLongKey());
                var chunkZ = ChunkUtils.longToChunkZ(entry.getLongKey());
                drawing.drawingCache.addHighlight(chunkX, chunkZ, (int) entry.getLongValue(), dimension);
            }
        }
    }

    public record LineDrawingOperation(Line line, Integer previousColor, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            if (previousColor == null) {
                drawing.drawingCache.removeLine(line, dimension);
            } else {
                drawing.drawingCache.addLine(line, previousColor, dimension);
            }
        }
    }

    public record EllipseDrawingOperation(Ellipse ellipse, Integer previousColor, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            if (previousColor == null) {
                drawing.drawingCache.removeEllipse(ellipse, dimension);
            } else {
                drawing.drawingCache.addEllipse(ellipse, previousColor, dimension);
            }
        }
    }

    public record TextDrawingOperation(Text text, Text previousText, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            if (previousText == null) {
                drawing.drawingCache.removeText(text.x(), text.z(), dimension);
            } else {
                drawing.drawingCache.addText(previousText, dimension);
            }
        }
    }

    public record EraseOperation(Long2LongMap highlights, Object2IntMap<Line> lines, Object2IntMap<Ellipse> ellipses, List<Text> texts, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            for (var entry : highlights.long2LongEntrySet()) {
                var chunkX = ChunkUtils.longToChunkX(entry.getLongKey());
                var chunkZ = ChunkUtils.longToChunkZ(entry.getLongKey());
                drawing.drawingCache.addHighlight(chunkX, chunkZ, (int) entry.getLongValue(), dimension);
            }
            for (var entry : lines.object2IntEntrySet()) {
                drawing.drawingCache.addLine(entry.getKey(), entry.getIntValue(), dimension);
            }
            for (var entry : ellipses.object2IntEntrySet()) {
                drawing.drawingCache.addEllipse(entry.getKey(), entry.getIntValue(), dimension);
            }
            for (var text : texts) {
                drawing.drawingCache.addText(text, dimension);
            }
        }
    }

    public static class DrawingOperationCollector {
        private final Long2LongMap replacedHighlights = new Long2LongOpenHashMap();
        private final LongSet addedHighlights = new LongOpenHashSet();
        private final Long2LongMap erasedHighlights = new Long2LongOpenHashMap();
        private Line line;
        private Integer replacedLineColor;
        private final Object2IntMap<Line> erasedLines = new Object2IntOpenHashMap<>();
        private Ellipse ellipse;
        private Integer replacedEllipseColor;
        private final Object2IntMap<Ellipse> erasedEllipses = new Object2IntOpenHashMap<>();
        private Text text;
        private Text replacedText;
        private final List<Text> erasedTexts = new ArrayList<>();
        private final ResourceKey<Level> dimension;
        public boolean erase;

        public DrawingOperationCollector(ResourceKey<Level> dimension, boolean erase) {
            this.dimension = dimension;
            this.erase = erase;
        }

        public void addHighlight(final long chunkPos, final Long previousColor) {
            if (replacedHighlights.containsKey(chunkPos) || addedHighlights.contains(chunkPos)) return;
            if (previousColor == null) {
                addedHighlights.add(chunkPos);
            } else {
                replacedHighlights.put(chunkPos, previousColor.longValue());
            }
        }

        public void addErasedHighlight(final long chunkPos, final long color) {
            erasedHighlights.put(chunkPos, color);
        }

        public void addErasedHighlights(final Long2LongMap highlights) {
            erasedHighlights.putAll(highlights);
        }

        public void addLine(final Line line, final Integer previousColor) {
            this.line = line;
            this.replacedLineColor = previousColor;
        }

        public void addErasedLine(final Line line, final int color) {
            erasedLines.put(line, color);
        }

        public void addEllipse(final Ellipse ellipse, final Integer previousColor) {
            this.ellipse = ellipse;
            this.replacedEllipseColor = previousColor;
        }

        public void addErasedEllipse(final Ellipse ellipse, final int color) {
            erasedEllipses.put(ellipse, color);
        }

        public void addText(final Text text, final Text previousText) {
            this.text = text;
            this.replacedText = previousText;
        }

        public void addErasedText(final Text text) {
            erasedTexts.add(text);
        }

        public DrawingOperation collect() {
            if (erase) {
                if (erasedHighlights.isEmpty() && erasedLines.isEmpty() && erasedEllipses.isEmpty() && erasedTexts.isEmpty()) return null;
                return new EraseOperation(erasedHighlights, erasedLines, erasedEllipses, erasedTexts, dimension);
            }
            if (!replacedHighlights.isEmpty() || !addedHighlights.isEmpty()) {
                return new HighlightDrawingOperation(replacedHighlights, addedHighlights, dimension);
            } else if (line != null) {
                // only one line at a time
                return new LineDrawingOperation(line, replacedLineColor, dimension);
            } else if (ellipse != null) {
                // only one ellipse at a time
                return new EllipseDrawingOperation(ellipse, replacedEllipseColor, dimension);
            } else if (text != null) {
                // only one text at a time
                return new TextDrawingOperation(text, replacedText, dimension);
            }
            return null;
        }
    }

}
