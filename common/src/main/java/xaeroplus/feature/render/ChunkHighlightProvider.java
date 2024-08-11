package xaeroplus.feature.render;

import java.util.function.IntSupplier;

public record ChunkHighlightProvider(
    ChunkHighlightSupplier chunkHighlightSupplier,
    IntSupplier colorSupplier
) {}
