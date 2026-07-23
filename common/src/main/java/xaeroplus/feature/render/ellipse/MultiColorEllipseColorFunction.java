package xaeroplus.feature.render.ellipse;

@FunctionalInterface
public interface MultiColorEllipseColorFunction {
    int getColor(Ellipse ellipse, int value);
}
