package xaeroplus.feature.render.line;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineDrawFeature extends AbstractLineDrawFeature<List<Line>> {
    private final String id;
    private final LineProvider lineProvider;
    private final LineVertexBuffer drawBuffer = new LineVertexBuffer();

    public LineDrawFeature(final String id, final LineProvider lineProvider, int refreshIntervalMs) {
        super(refreshIntervalMs);
        this.id = id;
        this.lineProvider = lineProvider;
    }

    @Override
    public float lineWidth() {
        return lineProvider.lineWidthSupplier().getFloat();
    }

    @Override
    public List<Line> provideLinesInWindow(final int windowX, final int windowZ, final int windowSize, final ResourceKey<Level> dimension) {
        return lineProvider.lineSupplier().getLines(windowX, windowZ, windowSize, dimension);
    }

    @Override
    public List<Line> preProcessLines(final List<Line> lines, final int windowX, final int windowZ, final int windowSize) {
        if (lines.isEmpty()) return lines;
        var bounds = LinePreProcessor.windowBounds(windowX, windowZ, windowSize);
        List<Line> out = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            var processedLines = LinePreProcessor.clippedSplitOriented(lines.get(i), bounds);
            if (!processedLines.isEmpty()) {
                out.addAll(processedLines);
            }
        }
        return out;
    }

    @Override
    public List<Line> emptyLines() {
        return Collections.emptyList();
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
        int color = lineProvider.colorSupplier().getAsInt();
        if (ColorHelper.getA(color) == 0.0f) return;
        drawBuffer.setColor(color);
        preRender(ctx);
        drawBuffer.preRender(ctx, getLines());
        drawBuffer.render();
        postRender(ctx);
    }
}
