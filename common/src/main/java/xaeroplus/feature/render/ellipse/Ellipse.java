package xaeroplus.feature.render.ellipse;

public record Ellipse(int centerX, int centerZ, int radiusX, int radiusZ) {
    public Ellipse {
        if (radiusX <= 0) {
            throw new IllegalArgumentException("radiusX must be positive");
        }
        if (radiusZ <= 0) {
            throw new IllegalArgumentException("radiusZ must be positive");
        }
    }

    public boolean intersects(final int minX, final int maxX, final int minZ, final int maxZ) {
        return (long) centerX + radiusX >= minX
            && (long) centerX - radiusX <= maxX
            && (long) centerZ + radiusZ >= minZ
            && (long) centerZ - radiusZ <= maxZ;
    }
}
