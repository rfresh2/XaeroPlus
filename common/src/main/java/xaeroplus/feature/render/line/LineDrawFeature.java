package xaeroplus.feature.render.line;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.graphics.CustomRenderTypes;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineDrawFeature extends AbstractLineDrawFeature<List<Line>> {
    private final String id;
    private final LineProvider lineProvider;

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
    public List<Line> preProcessLines(final List<Line> lines) {
        if (lines.isEmpty()) return lines;
        List<Line> out = new ArrayList<>(lines);
        for (int i = 0; i < out.size(); i++) {
            Line line = out.get(i);
            var newLines = LinePreProcessor.ensureLength(line);
            if (!newLines.isEmpty()) {
                out.remove(i);
                out.addAll(i, newLines);
                i += newLines.size() - 1;
            }
        }
        out.replaceAll(LinePreProcessor::ensureOrientation);
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
    public void render(final DrawContext ctx) {
        int color = lineProvider.colorSupplier().getAsInt();
        var a = ColorHelper.getA(color);
        if (a == 0.0f) return;
        VertexConsumer lineBuffer = ctx.renderTypeBuffers().getBuffer(CustomRenderTypes.MAP_LINES);
        var r = ColorHelper.getR(color);
        var g = ColorHelper.getG(color);
        var b = ColorHelper.getB(color);
        var lines = getLines();
        for (int j = 0; j < lines.size(); j++) {
            var line = lines.get(j);
            int x1 = ctx.worldmap() ? line.x2() : line.x1();
            int z1 = ctx.worldmap() ? line.z2() : line.z1();
            int x2 = ctx.worldmap() ? line.x1() : line.x2();
            int z2 = ctx.worldmap() ? line.z1() : line.z2();
            DrawHelper.addColoredLineToExistingBuffer(
                ctx.matrixStack().last(), lineBuffer,
                x1, z1, x2, z2,
                r, g, b, a
            );
        }
    }

    @Override
    public void postRender(final DrawContext ctx) {
        ctx.renderTypeBuffers().endBatch(CustomRenderTypes.MAP_LINES);
    }
}
