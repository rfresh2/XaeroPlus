package xaeroplus.util;

import net.minecraft.client.gui.DrawContext;
import xaero.map.region.MapTileChunk;

import java.util.ArrayList;
import java.util.List;

public class GuiHelper {

    public static void drawMMBackground(final DrawContext guiGraphics, final int drawX, final int drawZ, final MapTileChunk chunk) {
        boolean[][] seenTiles = ((SeenChunksTrackingMapTileChunk) (Object) chunk).getSeenTiles();
        final List<Rect> rects = new ArrayList<>(32);
        for(int o = 0; o < 4; ++o) {
            for (int p = 0; p < 4; ++p) {
                if (seenTiles[o][p]) {
                    rects.add(new Rect(drawX + (o << 4), drawZ + (p << 4),
                                       drawX + ((o + 1) << 4), drawZ + ((p + 1) << 4)));
                }
            }
        }
        int color = ColorHelper.getColor(0, 0, 0, 255);
        for (Rect rect : rects) {
            guiGraphics.fill((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom, color);
        }
    }

    //    public static void drawRect(float left, float top, float right, float bottom, int color)
//    {
//        if (left < right)
//        {
//            float i = left;
//            left = right;
//            right = i;
//        }
//
//        if (top < bottom)
//        {
//            float j = top;
//            top = bottom;
//            bottom = j;
//        }
//
//        float f3 = (float)(color >> 24 & 255) / 255.0F;
//        float f = (float)(color >> 16 & 255) / 255.0F;
//        float f1 = (float)(color >> 8 & 255) / 255.0F;
//        float f2 = (float)(color & 255) / 255.0F;
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder bufferbuilder = tessellator.getBuffer();
//        GlStateManager.enableBlend();
//        GlStateManager.disableTexture2D();
//        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
//        GlStateManager.color(f, f1, f2, f3);
//        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
//        bufferbuilder.pos(left, bottom, 0.0D).endVertex();
//        bufferbuilder.pos(right, bottom, 0.0D).endVertex();
//        bufferbuilder.pos(right, top, 0.0D).endVertex();
//        bufferbuilder.pos(left, top, 0.0D).endVertex();
//        tessellator.draw();
//        GlStateManager.enableTexture2D();
//        GlStateManager.disableBlend();
//    }
//
//    public static void drawRectList(final List<Rect> rects, final int color) {
//        if (rects.isEmpty()) return;
//        float f3 = (float)(color >> 24 & 255) / 255.0F;
//        float f = (float)(color >> 16 & 255) / 255.0F;
//        float f1 = (float)(color >> 8 & 255) / 255.0F;
//        float f2 = (float)(color & 255) / 255.0F;
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder bufferbuilder = tessellator.getBuffer();
//        GlStateManager.enableBlend();
//        GlStateManager.disableTexture2D();
//        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
//        GlStateManager.color(f, f1, f2, f3);
//        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION);
//        rects.forEach(rect -> {
//            bufferbuilder.pos(rect.left, rect.bottom, 0.0D).endVertex();
//            bufferbuilder.pos(rect.right, rect.bottom, 0.0D).endVertex();
//            bufferbuilder.pos(rect.right, rect.top, 0.0D).endVertex();
//            bufferbuilder.pos(rect.left, rect.top, 0.0D).endVertex();
//        });
//        tessellator.draw();
//        GlStateManager.enableTexture2D();
//        GlStateManager.disableBlend();
//    }
//
    public static class Rect {
        public float left;
        public float top;
        public float right;
        public float bottom;

        public Rect(float left, float top, float right, float bottom) {
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
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
//
//    // skips all the unnecessary blending and texture stuff
//    public static void drawRectSimple(float left, float top, float right, float bottom, int color)
//    {
//        if (left < right)
//        {
//            float i = left;
//            left = right;
//            right = i;
//        }
//
//        if (top < bottom)
//        {
//            float j = top;
//            top = bottom;
//            bottom = j;
//        }
//
//        float f3 = (float)(color >> 24 & 255) / 255.0F;
//        float f = (float)(color >> 16 & 255) / 255.0F;
//        float f1 = (float)(color >> 8 & 255) / 255.0F;
//        float f2 = (float)(color & 255) / 255.0F;
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder bufferbuilder = tessellator.getBuffer();
//        GlStateManager.color(f, f1, f2, f3);
//        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
//        bufferbuilder.pos(left, bottom, 0.0D).endVertex();
//        bufferbuilder.pos(right, bottom, 0.0D).endVertex();
//        bufferbuilder.pos(right, top, 0.0D).endVertex();
//        bufferbuilder.pos(left, top, 0.0D).endVertex();
//        tessellator.draw();
//    }
//
//    public static void drawRectListSimple(final List<Rect> rects, final int color) {
//        if (rects.isEmpty()) return;
//        float f3 = (float)(color >> 24 & 255) / 255.0F;
//        float f = (float)(color >> 16 & 255) / 255.0F;
//        float f1 = (float)(color >> 8 & 255) / 255.0F;
//        float f2 = (float)(color & 255) / 255.0F;
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder bufferbuilder = tessellator.getBuffer();
//        GlStateManager.color(f, f1, f2, f3);
//        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION);
//        rects.forEach(rect -> {
//            bufferbuilder.pos(rect.left, rect.bottom, 0.0D).endVertex();
//            bufferbuilder.pos(rect.right, rect.bottom, 0.0D).endVertex();
//            bufferbuilder.pos(rect.right, rect.top, 0.0D).endVertex();
//            bufferbuilder.pos(rect.left, rect.top, 0.0D).endVertex();
//        });
//        tessellator.draw();
//    }
//
//    public static void drawMMBackground(final float drawX, final float drawZ, final float brightness, final MapTileChunk chunk) {
//        GlStateManager.disableBlend();
//        GlStateManager.color(1.0f, 0f, 0f, 1.0F);
//        boolean[][] seenTiles = ((SeenChunksTrackingMapTileChunk) (Object) chunk).getSeenTiles();
//        final List<Rect> rects = new ArrayList<>(32);
//        for(int o = 0; o < 4; ++o) {
//            for (int p = 0; p < 4; ++p) {
//                if (seenTiles[o][p]) {
//                    rects.add(new Rect(drawX + (o << 4), drawZ + (p << 4),
//                            drawX + ((o + 1) << 4), drawZ + ((p + 1) << 4)));
//                }
//            }
//        }
//        GuiHelper.drawRectListSimple(rects,
//                // these color values get drawn on top of with the map textures, alpha is important though
//                ColorHelper.getColor(0, 0, 0, 255));
//        GlStateManager.enableBlend();
//        GlStateManager.color(brightness, brightness, brightness, 1.0F);
//    }
//
//    public static void finishMMSetup(final int compatibilityVersion, final float brightness, final MapTileChunk chunk, final boolean zooming) {
//        GuiMap.restoreTextureStates();
//        if (compatibilityVersion >= 6) {
//            GuiMap.setupTextures(brightness);
//        }
//    }
}
