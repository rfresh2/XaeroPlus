package xaeroplus.feature.render.ellipse;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EllipseDrawFeature extends AbstractEllipseDrawFeature<List<Ellipse>> {
    private final String id;
    private final EllipseProvider ellipseProvider;
    private final EllipseVertexBuffer drawBuffer = new EllipseVertexBuffer();

    public EllipseDrawFeature(final String id, final EllipseProvider ellipseProvider, final int refreshIntervalMs) {
        super(refreshIntervalMs);
        this.id = id;
        this.ellipseProvider = ellipseProvider;
    }

    @Override
    public float thickness() {
        return ellipseProvider.thicknessSupplier().getFloat();
    }

    @Override
    public List<Ellipse> provideEllipsesInWindow(final int windowX, final int windowZ, final int windowSize, final ResourceKey<Level> dimension) {
        return ellipseProvider.ellipseSupplier().getEllipses(windowX, windowZ, windowSize, dimension);
    }

    @Override
    public List<Ellipse> preProcessEllipses(final List<Ellipse> ellipses, final int windowX, final int windowZ, final int windowSize) {
        if (ellipses.isEmpty()) return ellipses;
        var bounds = EllipsePreProcessor.windowBounds(windowX, windowZ, windowSize);
        var visibleEllipses = new ArrayList<Ellipse>(ellipses.size());
        for (var ellipse : ellipses) {
            if (bounds.intersects(ellipse)) {
                visibleEllipses.add(ellipse);
            }
        }
        return visibleEllipses;
    }

    @Override
    public List<Ellipse> emptyEllipses() {
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
        var color = ellipseProvider.colorSupplier().getAsInt();
        if (ColorHelper.getA(color) == 0.0f) return;
        drawBuffer.setColor(color);
        preRender(ctx);
        drawBuffer.preRender(ctx, getEllipses());
        drawBuffer.render();
        postRender();
    }
}
