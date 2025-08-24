package xaeroplus.feature.render;

import xaeroplus.feature.render.highlight.*;
import xaeroplus.feature.render.line.*;
import xaeroplus.feature.render.text.AsyncTextDrawFeature;
import xaeroplus.feature.render.text.DirectTextDrawFeature;
import xaeroplus.feature.render.text.TextSupplier;
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

    static DrawFeature text(
        String id,
        TextSupplier textSupplier
    ) {
        return new DirectTextDrawFeature(
            id,
            textSupplier
        );
    }

    static DrawFeature text(
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
