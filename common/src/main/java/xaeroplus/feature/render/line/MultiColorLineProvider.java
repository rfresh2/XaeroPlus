package xaeroplus.feature.render.line;

import xaeroplus.util.FloatSupplier;

public record MultiColorLineProvider(
    MultiColorLineSupplier lineSupplier,
    MultiColorLineColorFunction colorFunction,
    FloatSupplier lineWidthSupplier
) {
}
