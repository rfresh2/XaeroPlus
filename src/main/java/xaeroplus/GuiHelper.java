package xaeroplus;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.region.MapTileChunk;
import xaeroplus.util.SeenChunksTrackingMapTileChunk;

public class GuiHelper {
    public static void drawRect(float left, float top, float right, float bottom, int color)
    {
        if (left < right)
        {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom)
        {
            float j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(f, f1, f2, f3);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(left, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, top, 0.0D).endVertex();
        bufferbuilder.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // skips all the unnecessary blending and texture stuff
    public static void drawRectSimple(float left, float top, float right, float bottom, int color)
    {
        if (left < right)
        {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom)
        {
            float j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.color(f, f1, f2, f3);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(left, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, top, 0.0D).endVertex();
        bufferbuilder.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
    }

    public static void drawMMBackground(final float drawX, final float drawZ, final float minimapTileChunkSizeRect, final float brightness, final MapTileChunk chunk, final MapProcessor mapProcessor) {
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 0f, 0f, 1.0F);
        final float minimapTileSizeRect = minimapTileChunkSizeRect / 4;
        boolean[][] seenTiles = ((SeenChunksTrackingMapTileChunk) (Object) chunk).getSeenTiles();
        for(int o = 0; o < 4; ++o) {
            for (int p = 0; p < 4; ++p) {
                if (seenTiles[o][p]) {
                    GuiHelper.drawRectSimple(drawX + (o * minimapTileSizeRect), drawZ + (p * minimapTileSizeRect),
                            drawX + ((o + 1) * minimapTileSizeRect), drawZ + ((p + 1) * minimapTileSizeRect),
                            // these color values get drawn on top of with the map textures, alpha is important though
                            XaeroPlus.getColor(0, 0, 0, 255));
                }
            }
        }
        GlStateManager.enableBlend();
        GlStateManager.color(brightness, brightness, brightness, 1.0F);
    }

    public static void finishMMSetup(final int compatibilityVersion, final float brightness, final MapTileChunk chunk, final boolean zooming) {
        GuiMap.restoreTextureStates();
        if (compatibilityVersion >= 6) {
            GuiMap.setupTextures(brightness);
        }
    }
}
