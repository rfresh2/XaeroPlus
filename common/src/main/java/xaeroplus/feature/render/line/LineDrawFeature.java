package xaeroplus.feature.render.line;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ChunkUtils;
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
    public List<Line> preProcessLines(final List<Line> lines, final int windowX, final int windowZ, final int windowSize) {
        if (lines.isEmpty()) return lines;
        List<Line> out = new ArrayList<>(lines);
        int windowXMin = ChunkUtils.regionCoordToCoord(windowX - windowSize);
        int windowZMin = ChunkUtils.regionCoordToCoord(windowZ - windowSize);
        int windowXMax = ChunkUtils.regionCoordToCoord(windowX + windowSize);
        int windowZMax = ChunkUtils.regionCoordToCoord(windowZ + windowSize);
        out.removeIf(l -> !l.lineClip(windowXMin, windowXMax, windowZMin, windowZMax));
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
        preRender(ctx);
        var r = ColorHelper.getR(color);
        var g = ColorHelper.getG(color);
        var b = ColorHelper.getB(color);
        var lines = getLines();
        if (!lines.isEmpty()) {
            var bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int j = 0; j < lines.size(); j++) {
                var line = lines.get(j);
                int x1 = ctx.worldmap() ? line.x2() : line.x1();
                int z1 = ctx.worldmap() ? line.z2() : line.z1();
                int x2 = ctx.worldmap() ? line.x1() : line.x2();
                int z2 = ctx.worldmap() ? line.z1() : line.z2();
                DrawHelper.addColoredLineQuadToExistingBuffer(
                    ctx.matrixStack().last(), bufferBuilder,
                    x1, z1, x2, z2,
                    r, g, b, a
                );
            }
            var meshData = bufferBuilder.buildOrThrow();
            RenderSystem.setShader(() -> XaeroPlusShaders.LINES_SHADER);
            BufferUploader.drawWithShader(meshData);
        }
        postRender(ctx);
    }
}
