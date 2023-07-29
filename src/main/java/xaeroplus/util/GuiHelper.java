package xaeroplus.util;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import xaero.map.gui.GuiMap;
import xaero.map.region.MapTileChunk;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.List;
import java.util.function.BiFunction;

import static org.lwjgl.opengl.GL11.GL_QUADS;

public class GuiHelper {

    public static void drawRectList(final List<Rect> rects, final int color) {
        if (rects.isEmpty()) return;
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
        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION);
        rects.forEach(rect -> {
            bufferbuilder.pos(rect.left, rect.bottom, 0.0D).endVertex();
            bufferbuilder.pos(rect.right, rect.bottom, 0.0D).endVertex();
            bufferbuilder.pos(rect.right, rect.top, 0.0D).endVertex();
            bufferbuilder.pos(rect.left, rect.top, 0.0D).endVertex();
        });
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawHighlightAtChunkPosList(final List<HighlightAtChunkPos> rects,
                                                   final int camX,
                                                   final int camZ,
                                                   final int color) {
        if (rects.isEmpty()) return;
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
        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION);
        rects.forEach(c -> {
            final float left = (float) ((c.x << 4) - camX);
            final float top = (float) ((c.z << 4) - camZ);
            final float right = left + 16;
            final float bottom = top + 16;
            bufferbuilder.pos(left, bottom, 0.0D).endVertex();
            bufferbuilder.pos(right, bottom, 0.0D).endVertex();
            bufferbuilder.pos(right, top, 0.0D).endVertex();
            bufferbuilder.pos(left, top, 0.0D).endVertex();
        });
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static class Rect {
        public float left;
        public float top;
        public float right;
        public float bottom;

        public Rect(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    public static void drawMMBackground(final float drawX, final float drawZ, final float brightness, final MapTileChunk chunk) {
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 0f, 0f, 1.0F);
        boolean[][] seenTiles = ((SeenChunksTrackingMapTileChunk) (Object) chunk).getSeenTiles();

        drawCombinedRectangles(
            (x, z) -> seenTiles[x][z],
            drawX, drawZ, 0, 0, ColorHelper.getColor(0, 0, 0, 255)
        );

        GlStateManager.enableBlend();
        GlStateManager.color(brightness, brightness, brightness, 1.0F);
    }


    public static void drawMMHighlights(BiFunction<Integer, Integer, Boolean> highlightFunc,
                                        int drawX,
                                        int drawZ,
                                        int chunkBaseX,
                                        int chunkBaseZ,
                                        int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        drawCombinedRectangles(
            highlightFunc,
            drawX, drawZ, chunkBaseX, chunkBaseZ, color
        );
    }

    public static void drawCombinedRectangles(BiFunction<Integer, Integer, Boolean> highlightFunc,
                                              float drawX,
                                              float drawZ,
                                              int chunkBaseX,
                                              int chunkBaseZ,
                                              int color) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        float a = (float) (color >> 24 & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.color(r, g, b, a);
        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION);

        float combinedLeft = 0;
        float combinedTop = 0;
        float combinedRight = 0;
        float combinedBottom = 0;
        boolean rowStarted = false;

        for (int z = 0; z < 4; ++z) {
            for (int x = 0; x < 4; ++x) {
                int chunkPosX = chunkBaseX + x;
                int chunkPosZ = chunkBaseZ + z;
                if (highlightFunc.apply(chunkPosX, chunkPosZ)) {
                    float left = drawX + 16 * x;
                    float top = drawZ + 16 * z;
                    float right = left + 16;
                    float bottom = top + 16;
                    if (!rowStarted) {
                        // Start a new row
                        combinedLeft = left;
                        combinedTop = top;
                        combinedRight = right;
                        combinedBottom = bottom;
                        rowStarted = true;
                    } else {
                        // Continue combining in the same row
                        combinedRight = right;
                        combinedBottom = Math.max(bottom, combinedBottom);
                    }
                } else if (rowStarted) {
                    // Stop combining and draw the previous row
                    bufferbuilder.pos(combinedLeft, combinedBottom, 0.0D).endVertex();
                    bufferbuilder.pos(combinedRight, combinedBottom, 0.0D).endVertex();
                    bufferbuilder.pos(combinedRight, combinedTop, 0.0D).endVertex();
                    bufferbuilder.pos(combinedLeft, combinedTop, 0.0D).endVertex();
                    rowStarted = false;
                }
            }
            // End the row and draw the combined rectangles
            if (rowStarted) {
                bufferbuilder.pos(combinedLeft, combinedBottom, 0.0D).endVertex();
                bufferbuilder.pos(combinedRight, combinedBottom, 0.0D).endVertex();
                bufferbuilder.pos(combinedRight, combinedTop, 0.0D).endVertex();
                bufferbuilder.pos(combinedLeft, combinedTop, 0.0D).endVertex();
                rowStarted = false;
            }
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void finishMMSetup(final int compatibilityVersion, final float brightness, final MapTileChunk chunk, final boolean zooming) {
        GuiMap.restoreTextureStates();
        if (compatibilityVersion >= 6) {
            GuiMap.setupTextures(brightness);
        }
    }
}
