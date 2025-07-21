package xaeroplus.feature.render.highlight;

@FunctionalInterface
public interface MultiColorHighlightColorFunction {
    int getColor(long chunkPos, long value);
}
