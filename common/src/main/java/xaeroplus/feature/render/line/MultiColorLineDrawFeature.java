package xaeroplus.feature.render.line;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.graphics.CustomRenderTypes;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.util.ColorHelper;

import java.util.List;

public class MultiColorLineDrawFeature extends AbstractLineDrawFeature<Object2IntMap<Line>> {
    private final String id;
    private final MultiColorLineProvider lineProvider;

    public MultiColorLineDrawFeature(final String id, final MultiColorLineProvider lineProvider, int refreshIntervalMs) {
        super(refreshIntervalMs);
        this.id = id;
        this.lineProvider = lineProvider;
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
    public Object2IntMap<Line> preProcessLines(final Object2IntMap<Line> lines) {
        if (lines.isEmpty()) return Object2IntMaps.emptyMap();
        Object2IntMap<Line> out = new Object2IntOpenHashMap<>(lines.size());
        var it = Object2IntMaps.fastIterator(lines);
        while (it.hasNext()) {
            var entry = it.next();
            Line line = entry.getKey();
            List<Line> newLines = LinePreProcessor.ensureLength(line);
            if (!newLines.isEmpty()) {
                for (Line newLine : newLines) {
                    out.put(LinePreProcessor.ensureOrientation(newLine), entry.getIntValue());
                }
            } else {
                out.put(LinePreProcessor.ensureOrientation(line), entry.getIntValue());
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
    public void render(final DrawContext ctx) {
        float a = lineProvider.colorAlphaSupplier().getAsInt() / 255.0f;
        if (a == 0) return;
        preRender(ctx);
        VertexConsumer lineBuffer = ctx.renderTypeBuffers().getBuffer(CustomRenderTypes.MAP_LINES);
        var lines = getLines();
        var it = Object2IntMaps.fastIterator(lines);
        while (it.hasNext()) {
            var entry = it.next();
            var line = entry.getKey();
            var color = entry.getIntValue();
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
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
        if (!lines.isEmpty()) {
            ctx.renderTypeBuffers().endBatch(CustomRenderTypes.MAP_LINES);
        }
    }
}
