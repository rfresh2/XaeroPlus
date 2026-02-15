package xaeroplus.feature.render.line;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.feature.render.DrawContext;

public class MultiColorLineDrawFeature extends AbstractLineDrawFeature<Object2IntMap<Line>> {
    private final String id;
    private final MultiColorLineProvider lineProvider;
    private final MultiColorLineVertexBuffer drawBuffer;

    public MultiColorLineDrawFeature(final String id, final MultiColorLineProvider lineProvider, int refreshIntervalMs) {
        super(refreshIntervalMs);
        this.id = id;
        this.lineProvider = lineProvider;
        this.drawBuffer = new MultiColorLineVertexBuffer(lineProvider.colorFunction());
    }

    @Override
    public float lineWidth() {
        return lineProvider.lineWidthSupplier().getFloat();
    }

    @Override
    public Object2IntMap<Line> provideLinesInWindow(final int windowX, final int windowZ, final int windowSize, final ResourceKey<Level> dimension) {
        return lineProvider.lineSupplier().getLines(windowX, windowZ, windowSize, dimension);
    }

    @Override
    public Object2IntMap<Line> preProcessLines(final Object2IntMap<Line> lines, final int windowX, final int windowZ, final int windowSize) {
        if (lines.isEmpty()) return Object2IntMaps.emptyMap();
        Object2IntMap<Line> out = new Object2IntOpenHashMap<>(lines.size());
        var bounds = LinePreProcessor.windowBounds(windowX, windowZ, windowSize);
        var it = Object2IntMaps.fastIterator(lines);
        while (it.hasNext()) {
            var entry = it.next();
            var processedLines = LinePreProcessor.clippedSplitOriented(entry.getKey(), bounds);
            for (int i = 0; i < processedLines.size(); i++) {
                out.put(processedLines.get(i), entry.getIntValue());
            }
        }
        return out;
    }

    @Override
    public Object2IntMap<Line> emptyLines() {
        return Object2IntMaps.emptyMap();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    protected void markDrawBufferStale() {
        drawBuffer.markStale();
    }

    @Override
    protected void closeDrawBuffer() {
        drawBuffer.close();
    }

    @Override
    public void render(final DrawContext ctx) {
        preRender(ctx);
        drawBuffer.preRender(ctx, getLines());
        drawBuffer.render();
        postRender(ctx);
    }
}
