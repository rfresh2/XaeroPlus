package xaeroplus.feature.render.ellipse;

import xaeroplus.util.FloatSupplier;

import java.util.function.IntSupplier;

public record EllipseProvider(
    EllipseSupplier ellipseSupplier,
    IntSupplier colorSupplier,
    FloatSupplier thicknessSupplier
) { }
