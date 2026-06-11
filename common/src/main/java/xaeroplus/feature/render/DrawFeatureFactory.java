package xaeroplus.feature.render;

import xaeroplus.feature.render.highlight.*;
import xaeroplus.feature.render.line.*;
import xaeroplus.feature.render.text.AsyncTextDrawFeature;
import xaeroplus.feature.render.text.DirectTextDrawFeature;
import xaeroplus.feature.render.text.TextSupplier;
import xaeroplus.util.FloatSupplier;

import java.util.function.IntSupplier;

public interface DrawFeatureFactory {

    /**
     * Refreshed on MC render thread
     * Single color across all highlights
     * Color function is called each frame
     */
    static DrawFeature chunkHighlights(
        String id,
        DirectChunkHighlightSupplier chunkHighlightSupplier,
        IntSupplier colorSupplier,
        int refreshIntervalMs
    ) {
        return new DirectChunkHighlightDrawFeature(
            id,
            new HighlightVertexBuffer(),
            new DirectChunkHighlightProvider(
                chunkHighlightSupplier,
                colorSupplier
            ),
            refreshIntervalMs
        );
    }

    /**
     * Refreshed on MC render thread
     * Color function per-chunk
     * Color function is called only on refresh
     */
    static DrawFeature multiColorChunkHighlights(
        String id,
        DirectChunkHighlightSupplier chunkHighlightSupplier,
        MultiColorHighlightColorFunction colorFunction,
        int refreshIntervalMs
    ) {
        return new DirectChunkHighlightDrawFeature(
            id,
            new MultiColorHighlightVertexBuffer(colorFunction),
            new DirectChunkHighlightProvider(
                chunkHighlightSupplier,
                () -> 0
            ),
            refreshIntervalMs
        );
    }

    /**
     * Refreshed async, not on the MC render thread
     * Single color across all highlights
     * Color function is called each frame
     */
    static DrawFeature asyncChunkHighlights(
        String id,
        AsyncChunkHighlightSupplier chunkHighlightSupplier,
        IntSupplier colorSupplier
    ) {
        return new AsyncChunkHighlightDrawFeature(
            id,
            new HighlightVertexBuffer(),
            new AsyncChunkHighlightProvider(
                chunkHighlightSupplier,
                colorSupplier
            )
        );
    }

    /**
     * Refreshed async, not on the MC render thread
     * Color function per-chunk
     * Color function is called only on refresh
     */
    static DrawFeature multiColorAsyncChunkHighlights(
        String id,
        AsyncChunkHighlightSupplier chunkHighlightSupplier,
        MultiColorHighlightColorFunction colorFunction
    ) {
        return new AsyncChunkHighlightDrawFeature(
            id,
            new MultiColorHighlightVertexBuffer(colorFunction),
            new AsyncChunkHighlightProvider(
                chunkHighlightSupplier,
                () -> 0
            )
        );
    }

    /**
     * Refreshed on MC render thread
     * Single color across all lines
     * Color function is called each frame
     */
    static DrawFeature lines(
        String id,
        LineSupplier lineSupplier,
        IntSupplier colorSupplier,
        FloatSupplier lineWidthSupplier,
        int refreshIntervalMs
    ) {
        return new LineDrawFeature(
            id,
            new LineProvider(
                lineSupplier,
                colorSupplier,
                lineWidthSupplier
            ),
            refreshIntervalMs
        );
    }

    /**
     * Refreshed on MC render thread
     * Color function per-line
     * Color function is called only on refresh
     */
    static DrawFeature multiColorLines(
        String id,
        MultiColorLineSupplier lineSupplier,
        MultiColorLineColorFunction colorFunction,
        FloatSupplier lineWidthSupplier,
        int refreshIntervalMs
    ) {
        return new MultiColorLineDrawFeature(
            id,
            new MultiColorLineProvider(
                lineSupplier,
                colorFunction,
                lineWidthSupplier
            ),
            refreshIntervalMs
        );
    }

    /**
     * Refreshed every frame on MC render thread
     */
    static DrawFeature text(
        String id,
        TextSupplier textSupplier
    ) {
        return new DirectTextDrawFeature(
            id,
            textSupplier
        );
    }

    /**
     * Refreshed async, not on the MC render thread
     */
    static DrawFeature asyncText(
        String id,
        TextSupplier textSupplier,
        int refreshIntervalMs
    ) {
        return new AsyncTextDrawFeature(
            id,
            textSupplier,
            refreshIntervalMs
        );
    }
}
