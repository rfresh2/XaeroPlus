package xaeroplus.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import xaero.map.region.MapTileChunk;

public class GuiHelper {

    public static void drawMMBackground(final DrawContext guiGraphics, final int drawX, final int drawZ, final MapTileChunk chunk) {
        boolean[][] seenTiles = ((SeenChunksTrackingMapTileChunk) (Object) chunk).getSeenTiles();
        int combinedLeft = 0;
        int combinedTop = 0;
        int combinedRight = 0;
        int combinedBottom = 0;
        boolean rowStarted = false;

        for(int o = 0; o < 4; ++o) {
            for (int p = 0; p < 4; ++p) {
                if (seenTiles[o][p]) {
                    int left = drawX + (o << 4);
                    int top = drawZ + (p << 4);
                    int right = drawX + ((o + 1) << 4);
                    int bottom = drawZ + ((p + 1) << 4);
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
                    guiGraphics.fill(combinedLeft,
                                     combinedTop,
                                     combinedRight,
                                     combinedBottom,
                                     ColorHelper.getColor(0, 0, 0, 255));
                    rowStarted = false;
                }
            }
            // End the row and draw the combined rectangles
            if (rowStarted) {
                guiGraphics.fill(combinedLeft,
                                 combinedTop,
                                 combinedRight,
                                 combinedBottom,
                                 ColorHelper.getColor(0, 0, 0, 255));
                rowStarted = false;
            }
        }
    }

    public static void addMMBackgroundToBuffer(Matrix4f matrix, VertexConsumer bufferBuilder, final int drawX, final int drawZ, final MapTileChunk chunk) {
        boolean[][] seenTiles = ((SeenChunksTrackingMapTileChunk) (Object) chunk).getSeenTiles();
        int combinedLeft = 0;
        int combinedTop = 0;
        int combinedRight = 0;
        int combinedBottom = 0;
        boolean rowStarted = false;

        for(int o = 0; o < 4; ++o) {
            for (int p = 0; p < 4; ++p) {
                if (seenTiles[o][p]) {
                    int left = drawX + (o << 4);
                    int top = drawZ + (p << 4);
                    int right = drawX + ((o + 1) << 4);
                    int bottom = drawZ + ((p + 1) << 4);
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
                    fillIntoExistingBuffer(matrix,
                                           bufferBuilder,
                                           combinedLeft,
                                           combinedTop,
                                           combinedRight,
                                           combinedBottom,
                                           0,
                                           0,
                                           0,
                                           1);
                    rowStarted = false;
                }
            }
            // End the row and draw the combined rectangles
            if (rowStarted) {
                fillIntoExistingBuffer(matrix,
                                       bufferBuilder,
                                       combinedLeft,
                                       combinedTop,
                                       combinedRight,
                                       combinedBottom,
                                       0,
                                       0,
                                       0,
                                       1);
                rowStarted = false;
            }
        }
    }

    public static void fillIntoExistingBuffer(Matrix4f matrix, VertexConsumer bufferBuilder, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).next();
    }
}
