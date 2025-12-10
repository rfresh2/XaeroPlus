package xaeroplus.feature.render.line;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaero.common.graphics.CustomRenderTypes;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.util.ChunkUtils;
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
    public Object2IntMap<Line> preProcessLines(final Object2IntMap<Line> lines, final int windowX, final int windowZ, final int windowSize) {
        if (lines.isEmpty()) return Object2IntMaps.emptyMap();
        Object2IntMap<Line> out = new Object2IntOpenHashMap<>(lines.size());
        int windowXMin = ChunkUtils.regionCoordToCoord(windowX - windowSize);
        int windowZMin = ChunkUtils.regionCoordToCoord(windowZ - windowSize);
        int windowXMax = ChunkUtils.regionCoordToCoord(windowX + windowSize);
        int windowZMax = ChunkUtils.regionCoordToCoord(windowZ + windowSize);
        var it = Object2IntMaps.fastIterator(lines);
        while (it.hasNext()) {
            var entry = it.next();
            Line line = entry.getKey();
            if (!line.lineClip(windowXMin, windowXMax, windowZMin, windowZMax)) continue;
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
        preRender(ctx);
        VertexConsumer lineBuffer = ctx.renderTypeBuffers().getBuffer(CustomRenderTypes.MAP_LINES);
        float lineWidthScale = 16f * (float) Mth.clamp(
            lineWidth() * ctx.fboScale(),
            0.1f * (ctx.worldmap() ? 1.0f : Globals.minimapScaleMultiplier),
            1000.0f
        );
        lineBuffer.setLineWidth(lineWidthScale);
        var lines = getLines();
        var it = Object2IntMaps.fastIterator(lines);
        boolean hasLines = false;
        while (it.hasNext()) {
            var entry = it.next();
            var line = entry.getKey();
            var color = lineProvider.colorFunction().getColor(line, entry.getIntValue());
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            var a = ColorHelper.getA(color);
            if (a == 0) continue;
            int x1 = ctx.worldmap() ? line.x2() : line.x1();
            int z1 = ctx.worldmap() ? line.z2() : line.z1();
            int x2 = ctx.worldmap() ? line.x1() : line.x2();
            int z2 = ctx.worldmap() ? line.z1() : line.z2();
            DrawHelper.addColoredLineToExistingBuffer(
                ctx.matrixStack().last(), lineBuffer,
                x1, z1, x2, z2,
                r, g, b, a
            );
            hasLines = true;
        }
        if (hasLines) {
            ctx.renderTypeBuffers().endBatch(CustomRenderTypes.MAP_LINES);
        }
    }
}
