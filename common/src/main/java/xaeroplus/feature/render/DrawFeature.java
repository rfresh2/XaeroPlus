package xaeroplus.feature.render;

public interface DrawFeature {
    String id();

    void preRender(DrawContext ctx);

    void render(DrawContext ctx);

    void postRender(DrawContext ctx);

    void invalidateCache();

    void close();
}
