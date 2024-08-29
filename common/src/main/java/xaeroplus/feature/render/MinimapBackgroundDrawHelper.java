package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import xaero.map.region.MapTileChunk;
import xaeroplus.feature.extensions.SeenChunksTrackingMapTileChunk;
import xaeroplus.util.ChunkUtils;

public class MinimapBackgroundDrawHelper {

    public static void addMMBackgroundToBuffer(Matrix4f matrix, VertexConsumer bufferBuilder, final int drawX, final int drawY, final MapTileChunk chunk) {
        // Most of the cpu cost in this method is submitting vertices to the buffer
        // fewer vertices = less cpu cost
        // this is drawing at the MapTileChunks level (4x4 MC chunks)
        // MapTileChunks without texture perform best as there's no bg to draw
        // if we did drawing at the MapRegion level we could potentially optimize to larger rects
        boolean[][] seenTiles = ((SeenChunksTrackingMapTileChunk) chunk).getSeenTiles();

        boolean hasEmptyTiles = false;
        boolean hasTexturedTiles = false;
        for (int tileIndexX = 0; tileIndexX < 4; tileIndexX++) {
            for (int tileIndexZ = 0; tileIndexZ < 4; tileIndexZ++) {
                if (seenTiles[tileIndexX][tileIndexZ]) hasTexturedTiles = true;
                else hasEmptyTiles = true;
            }
        }
        if (!hasTexturedTiles) return;
        if (!hasEmptyTiles) { // draw bg on entire tile chunk
            drawBg(matrix, bufferBuilder, drawX, drawY, drawX + 64, drawY + 64);
            return;
        }

        for (int tileIndexX = 0; tileIndexX < 4; tileIndexX++) {
            int top = 0;
            int bottom = 0;
            boolean columnStarted = false;
            int left = drawX + ChunkUtils.mapTileCoordToCoord(tileIndexX);
            int right = left + 16;
            for (int tileIndexY = 0; tileIndexY < 4; tileIndexY++) {
                if (seenTiles[tileIndexX][tileIndexY]) {
                    int nextTop = drawY + ChunkUtils.mapTileCoordToCoord(tileIndexY);
                    bottom = nextTop + 16;
                    if (!columnStarted) {
                        top = nextTop;
                        columnStarted = true;
                    }
                } else if (columnStarted) { // end batch and draw the previous column
                    drawBg(matrix, bufferBuilder, left, top, right, bottom);
                    columnStarted = false;
                }
            }
            // finish if column was not ended
            if (columnStarted) drawBg(matrix, bufferBuilder, left, top, right, bottom);
        }
    }

    static void drawBg(Matrix4f matrix, VertexConsumer bufferBuilder, float x1, float y1, float x2, float y2) {
        fillIntoExistingBuffer(matrix, bufferBuilder, x1, y1, x2, y2, 0, 0, 0, 1);
    }

    public static void fillIntoExistingBuffer(Matrix4f matrix, VertexConsumer bufferBuilder, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        bufferBuilder.addVertex(matrix, x1, y2, 0.0F).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, x2, y2, 0.0F).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, x2, y1, 0.0F).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, x1, y1, 0.0F).setColor(r, g, b, a);
    }
}
