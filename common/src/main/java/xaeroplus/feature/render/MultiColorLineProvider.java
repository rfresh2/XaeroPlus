package xaeroplus.feature.render;

import xaeroplus.util.FloatSupplier;

import java.util.function.IntSupplier;

public record MultiColorLineProvider(
    MultiColorLineSupplier lineSupplier,
    IntSupplier colorAlphaSupplier,
    FloatSupplier lineWidthSupplier
) {
}
