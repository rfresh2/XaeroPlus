package xaeroplus.feature.render;

import xaeroplus.util.FloatSupplier;

import java.util.function.IntSupplier;

public record ColoredLineProvider(
    ColoredLineSupplier lineSupplier,
    IntSupplier colorAlphaSupplier,
    FloatSupplier lineWidthSupplier
) {
}
