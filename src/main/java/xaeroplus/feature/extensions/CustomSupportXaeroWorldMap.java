package xaeroplus.feature.extensions;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import xaero.common.XaeroMinimapSession;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.map.MapProcessor;

public interface CustomSupportXaeroWorldMap {
    void drawMinimapWithDrawContext(
            DrawContext guiGraphics,
            XaeroMinimapSession minimapSession,
            MatrixStack matrixStack,
            MinimapRendererHelper helper,
            int xFloored,
            int zFloored,
            int minViewX,
            int minViewZ,
            int maxViewX,
            int maxViewZ,
            boolean zooming,
            double zoom,
            double mapDimensionScale,
            VertexConsumer overlayBufferBuilder,
            MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers);

    void renderChunksWithDrawContext(
        DrawContext guiGraphics,
        MatrixStack matrixStack,
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        int minViewX,
        int maxViewX,
        int minViewZ,
        int maxViewZ,
        MapProcessor mapProcessor,
        int renderedCaveLayer,
        boolean shouldRequestLoading,
        boolean reloadEverything,
        int globalReloadVersion,
        int globalRegionCacheHashCode,
        int globalCaveStart,
        int globalCaveDepth,
        boolean playerIsMoving,
        boolean noCaveMaps,
        boolean slimeChunks,
        int chunkX,
        int chunkZ,
        int tileX,
        int tileZ,
        int insideX,
        int insideZ,
        Long seed,
        MultiTextureRenderTypeRenderer mapWithLightRenderer,
        MultiTextureRenderTypeRenderer mapNoLightRenderer,
        MinimapRendererHelper helper,
        VertexConsumer overlayBufferBuilder
    );
}
