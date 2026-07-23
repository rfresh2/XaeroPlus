package xaeroplus.feature.render.ellipse;

import xaeroplus.util.ChunkUtils;

public class EllipsePreProcessor {
    public record WindowBounds(int minX, int maxX, int minZ, int maxZ) {
        public boolean intersects(final Ellipse ellipse) {
            return ellipse.intersects(minX, maxX, minZ, maxZ);
        }
    }

    public static WindowBounds windowBounds(final int windowX, final int windowZ, final int windowSize) {
        var minX = ChunkUtils.regionCoordToCoord(windowX - windowSize);
        var minZ = ChunkUtils.regionCoordToCoord(windowZ - windowSize);
        var maxX = ChunkUtils.regionCoordToCoord(windowX + windowSize);
        var maxZ = ChunkUtils.regionCoordToCoord(windowZ + windowSize);
        return new WindowBounds(minX, maxX, minZ, maxZ);
    }
}
