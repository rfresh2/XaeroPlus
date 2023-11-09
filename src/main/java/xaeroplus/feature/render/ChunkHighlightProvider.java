package xaeroplus.feature.render;

import java.util.function.Supplier;

public record ChunkHighlightProvider(
    ChunkHighlightPredicate chunkHighlightPredicate,
    Supplier<Integer> colorSupplier
) {
}

