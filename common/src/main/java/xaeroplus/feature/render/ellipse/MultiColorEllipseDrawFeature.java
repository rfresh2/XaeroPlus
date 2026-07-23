package xaeroplus.feature.render.ellipse;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.feature.render.DrawContext;

public class MultiColorEllipseDrawFeature extends AbstractEllipseDrawFeature<Object2IntMap<Ellipse>> {
    private final String id;
    private final MultiColorEllipseProvider ellipseProvider;
    private final MultiColorEllipseVertexBuffer drawBuffer;

    public MultiColorEllipseDrawFeature(final String id, final MultiColorEllipseProvider ellipseProvider, final int refreshIntervalMs) {
        super(refreshIntervalMs);
        this.id = id;
        this.ellipseProvider = ellipseProvider;
        drawBuffer = new MultiColorEllipseVertexBuffer(ellipseProvider.colorFunction());
    }

    @Override
    public float thickness() {
        return ellipseProvider.thicknessSupplier().getFloat();
    }

    @Override
    public Object2IntMap<Ellipse> provideEllipsesInWindow(final int windowX, final int windowZ, final int windowSize, final ResourceKey<Level> dimension) {
        return ellipseProvider.ellipseSupplier().getEllipses(windowX, windowZ, windowSize, dimension);
    }

    @Override
    public Object2IntMap<Ellipse> preProcessEllipses(final Object2IntMap<Ellipse> ellipses, final int windowX, final int windowZ, final int windowSize) {
        if (ellipses.isEmpty()) return Object2IntMaps.emptyMap();
        var bounds = EllipsePreProcessor.windowBounds(windowX, windowZ, windowSize);
        var visibleEllipses = new Object2IntOpenHashMap<Ellipse>(ellipses.size());
        var iterator = Object2IntMaps.fastIterator(ellipses);
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (bounds.intersects(entry.getKey())) {
                visibleEllipses.put(entry.getKey(), entry.getIntValue());
            }
        }
        return visibleEllipses;
    }

    @Override
    public Object2IntMap<Ellipse> emptyEllipses() {
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
        drawBuffer.preRender(ctx, getEllipses());
        drawBuffer.render(ctx, thicknessScale(ctx));
        postRender(ctx);
    }
}
