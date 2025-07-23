package xaeroplus.feature.render.line;

@FunctionalInterface
public interface MultiColorLineColorFunction {
    int getColor(Line line, int v);
}
