package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.drawing.DrawingCache;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.Module;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.DrawingMode;
import xaeroplus.util.GuiMapHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class Drawing extends Module {
    public final DrawingCache drawingCache = new DrawingCache("XaeroPlusDrawing");
    private Line inProgressLine = null;
    private int savedColorAlpha = 150;
    private final DrawingColorCycler drawingColorCycler = new DrawingColorCycler();
    private final int inProgressColorAlpha = 80;
    private final Deque<DrawingOperation> operationStack = new LinkedBlockingDeque<>();
    private DrawingOperationCollector operationCollector = null;
    public DrawingColorCycler getDrawingColorCycler() {
        return drawingColorCycler;
    }

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
                (line, v) -> ColorHelper.getColorWithAlpha(v, savedColorAlpha),
                () -> 0.5f,
                50
            )
        );
        Globals.drawManager.registry().register(
            DrawFeatureFactory.lines(
                "Drawing-lines-in-progress",
                this::getInProgressLines,
                () -> drawingColorCycler.getColorInt(inProgressColorAlpha),
                () -> 0.5f,
                1
            )
        );
        Globals.drawManager.registry().register(
            DrawFeatureFactory.multiColorChunkHighlights(
                "Drawing-highlights",
                drawingCache::getHighlights,
                (pos, t) -> ColorHelper.getColorWithAlpha((int) t, savedColorAlpha),
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
        drawingCache.handleWorldChange(event);
    }

    private Object2IntMap<Line> getSavedLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return drawingCache.getLines(dimension);
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

    public void addLine(final Line line, int color) {
        if (line.length() < 2) return;
        drawingCache.addLine(line, color, Globals.getCurrentDimensionId());
        if (operationCollector != null) {
            operationCollector.addLine(line);
        }
    }

    public void addLine(final Line line) {
        addLine(line, drawingColorCycler.getColorInt(inProgressColorAlpha));
    }

    public void addInfiniteLine(final Line line, int color) {
        if (line.length() < 2) return;
        var infLine = line.extrapolateToWorldBorder();
        drawingCache.addLine(infLine, color, Globals.getCurrentDimensionId());
        if (operationCollector != null) {
            operationCollector.addLine(infLine);
        }
    }

    public void addInfiniteLine(final Line line) {
        addInfiniteLine(line, drawingColorCycler.getColorInt(inProgressColorAlpha));
    }

    public void addHighlight(int chunkX, int chunkZ, int color) {
        drawingCache.addHighlight(chunkX, chunkZ, color, Globals.getCurrentDimensionId());
        if (operationCollector != null) {
            operationCollector.addHighlight(chunkX, chunkZ);
        }
    }

    public void addHighlight(int chunkX, int chunkZ) {
        addHighlight(chunkX, chunkZ, drawingColorCycler.getColor().getColor());
    }

    public void removeHighlight(final int chunkX, final int chunkZ) {
        if (!drawingCache.getHighlights(Globals.getCurrentDimensionId()).containsKey(ChunkUtils.chunkPosToLong(chunkX, chunkZ))) return;
        drawingCache.removeHighlight(chunkX, chunkZ, Globals.getCurrentDimensionId());
        if (operationCollector != null) {
            operationCollector.addHighlight(chunkX, chunkZ);
        }
    }

    public void addText(final Text text) {
        drawingCache.addText(text, Globals.getCurrentDimensionId());
        if (operationCollector != null) {
            operationCollector.addText(text);
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
                operationCollector.addText(text);
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

    public void removeLine(final int x, final int z) {
        Object2IntMap<Line> lines = drawingCache.getLines(Globals.getCurrentDimensionId());
        int maxX = x + 16;
        int maxZ = z + 16;
        Line sqLine1 = new Line(x, z, maxX, z);
        Line sqLine2 = new Line(x, z, x, maxZ);
        Line sqLine3 = new Line(maxX, z, maxX, maxZ);
        Line sqLine4 = new Line(x, maxZ, maxX, maxZ);
        List<Line> toRemove = new ArrayList<>();
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
                toRemove.add(line);
            }
        }
        for (Line line : toRemove) {
            drawingCache.removeLine(line, Globals.getCurrentDimensionId());
        }
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

    public void setOpacity(final int opacity) {
        this.savedColorAlpha = opacity;
    }

    private static final int SNAP_THRESHOLD = 10;
    public Line snap(int x1, int z1, int x2, int z2, double scale) {
        double scalar = 1.0 / scale;
        int threshold = Mth.clamp(Mth.floor(SNAP_THRESHOLD * scalar), 10, 1000);
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
                return new Line(x1, z1, x2, z1 + zDelta * zSignum);
            }
        }

        return new Line(x1, z1, x2, z2);
    }

    public void clearAll() {
        drawingCache.getAllHighlightCaches().forEach(c -> {
            var keySet = new LongArraySet(c.chunks.keySet());
            for (var key : keySet) {
                var x = ChunkUtils.longToChunkX(key);
                var z = ChunkUtils.longToChunkZ(key);
                c.removeHighlight(x, z);
            }
        });
        drawingCache.getAllLinesCaches().forEach(c -> {
            var lineSet = new ObjectArraySet<>(c.getLines().keySet());
            for (var line : lineSet) {
                c.removeLine(line);
            }
        });
        drawingCache.getAllTextsCaches().forEach(c -> {
            var keySet = new LongArraySet(c.getTexts().keySet());
            for (var key : keySet) {
                var x = ChunkUtils.longToChunkX(key);
                var z = ChunkUtils.longToChunkZ(key);
                c.removeText(x, z);
            }
        });
        operationCollector = null;
        operationStack.clear();
    }

    public interface DrawingOperation {
        void revert(Drawing drawing);
    }

    public record HighlightDrawingOperation(LongList chunks, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            for (var chunkLong : chunks) {
                var chunkX = ChunkUtils.longToChunkX(chunkLong);
                var chunkZ = ChunkUtils.longToChunkZ(chunkLong);
                drawing.drawingCache.removeHighlight(chunkX, chunkZ, dimension);
            }
        }
    }

    public record LineDrawingOperation(Line line, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            drawing.drawingCache.removeLine(line, dimension);
        }
    }

    public record TextDrawingOperation(Text text, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            drawing.drawingCache.removeText(text.x(), text.z(), dimension);
        }
    }

    public record EraseOperation(LongList chunks, List<Line> lines, List<Text> texts, ResourceKey<Level> dimension) implements DrawingOperation {
        @Override
        public void revert(Drawing drawing) {
            for (var chunkLong : chunks) {
                var chunkX = ChunkUtils.longToChunkX(chunkLong);
                var chunkZ = ChunkUtils.longToChunkZ(chunkLong);
                drawing.drawingCache.addHighlight(chunkX, chunkZ, drawing.drawingColorCycler.getColor().getColor(), dimension);
            }
            for (var line : lines) {
                drawing.drawingCache.addLine(line, drawing.drawingColorCycler.getColorInt(150), dimension);
            }
            for (var text : texts) {
                drawing.drawingCache.addText(text, dimension);
            }
        }
    }

    public static class DrawingOperationCollector {
        private final LongList chunks = new LongArrayList();
        private final List<Line> lines = new ArrayList<>();
        private final List<Text> texts = new ArrayList<>();
        private final ResourceKey<Level> dimension;
        public boolean erase;

        public DrawingOperationCollector(ResourceKey<Level> dimension, boolean erase) {
            this.dimension = dimension;
            this.erase = erase;
        }

        public void addHighlight(int chunkX, int chunkZ) {
            long chunkLong = ChunkUtils.chunkPosToLong(chunkX, chunkZ);
            chunks.add(chunkLong);
        }

        public void addLine(Line line) {
            lines.add(line);
        }

        public void addText(Text text) {
            texts.add(text);
        }

        public DrawingOperation collect() {
            if (erase) {
                return new EraseOperation(chunks, lines, texts, dimension);
            }
            if (!chunks.isEmpty()) {
                return new HighlightDrawingOperation(chunks, dimension);
            } else if (!lines.isEmpty()) {
                // only one line at a time
                return new LineDrawingOperation(lines.get(0), dimension);
            } else if (!texts.isEmpty()) {
                // only one text at a time
                return new TextDrawingOperation(texts.get(0), dimension);
            }
            return null;
        }
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
