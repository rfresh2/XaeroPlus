package xaeroplus.feature.render.ellipse;

import xaeroplus.util.FloatSupplier;

public record MultiColorEllipseProvider(
    MultiColorEllipseSupplier ellipseSupplier,
    MultiColorEllipseColorFunction colorFunction,
    FloatSupplier thicknessSupplier
) { }
