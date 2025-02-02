package xaeroplus.feature.render;

import java.util.function.IntSupplier;

public record AsyncChunkHighlightProvider(
    AsyncChunkHighlightSupplier chunkHighlightSupplier,
    IntSupplier colorSupplier
) {}
