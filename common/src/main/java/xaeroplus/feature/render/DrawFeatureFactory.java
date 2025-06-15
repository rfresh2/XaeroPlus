package xaeroplus.feature.render;

import xaeroplus.feature.render.highlight.*;
import xaeroplus.feature.render.line.*;
import xaeroplus.util.FloatSupplier;

import java.util.function.IntSupplier;

public interface DrawFeatureFactory {

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

    static DrawFeature multiColorChunkHighlights(
        String id,
        DirectChunkHighlightSupplier chunkHighlightSupplier,
        IntSupplier colorSupplier,
        int refreshIntervalMs
    ) {
        return new DirectChunkHighlightDrawFeature(
            id,
            new MultiColorHighlightVertexBuffer(),
            new DirectChunkHighlightProvider(
                chunkHighlightSupplier,
                colorSupplier
            ),
            refreshIntervalMs
        );
    }

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

    static DrawFeature multiColorAsyncChunkHighlights(
        String id,
        AsyncChunkHighlightSupplier chunkHighlightSupplier,
        IntSupplier colorSupplier
    ) {
        return new AsyncChunkHighlightDrawFeature(
            id,
            new MultiColorHighlightVertexBuffer(),
            new AsyncChunkHighlightProvider(
                chunkHighlightSupplier,
                colorSupplier
            )
        );
    }

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

    static DrawFeature multiColorLines(
        String id,
        MultiColorLineSupplier lineSupplier,
        IntSupplier colorSupplier,
        FloatSupplier lineWidthSupplier,
        int refreshIntervalMs
    ) {
        return new MultiColorLineDrawFeature(
            id,
            new MultiColorLineProvider(
                lineSupplier,
                colorSupplier,
                lineWidthSupplier
            ),
            refreshIntervalMs
        );
    }
}
