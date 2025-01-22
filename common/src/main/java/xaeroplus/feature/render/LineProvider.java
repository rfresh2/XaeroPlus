package xaeroplus.feature.render;

import xaeroplus.util.FloatSupplier;

import java.util.function.IntSupplier;

public record LineProvider(
    LineSupplier lineSupplier,
    IntSupplier colorSupplier,
    FloatSupplier lineWidthSupplier
) {
}
