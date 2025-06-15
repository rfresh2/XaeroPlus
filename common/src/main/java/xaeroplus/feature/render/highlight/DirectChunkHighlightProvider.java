package xaeroplus.feature.render.highlight;

import java.util.function.IntSupplier;

public record DirectChunkHighlightProvider(
    DirectChunkHighlightSupplier chunkHighlightSupplier,
    IntSupplier colorSupplier
) {}
