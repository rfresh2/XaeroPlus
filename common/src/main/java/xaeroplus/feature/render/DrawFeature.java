package xaeroplus.feature.render;

public interface DrawFeature {
    String id();

    void render(DrawContext ctx);

    void invalidateCache();

    void close();
}
