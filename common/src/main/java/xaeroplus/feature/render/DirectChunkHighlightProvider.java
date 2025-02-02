package xaeroplus.feature.render;

import java.util.function.IntSupplier;

public record DirectChunkHighlightProvider(
    DirectChunkHighlightSupplier chunkHighlightSupplier,
    IntSupplier colorSupplier
) {}
