package xaeroplus.feature.render.highlight;

import java.util.function.IntSupplier;

public record AsyncChunkHighlightProvider(
    AsyncChunkHighlightSupplier chunkHighlightSupplier,
    IntSupplier colorSupplier
) {}
