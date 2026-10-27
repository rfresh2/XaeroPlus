package xaeroplus.feature.render.async;

/**
 * Whether the world map screen is open (with async rendering enabled) and which blocks it shows.
 * Everything run in the background by async world map rendering is tied to the map screen:
 * work for blocks off screen is cancelled, and closing the screen cancels everything.
 */
public final class WorldMapScreenState {
    // Blocks of margin kept around the visible area: one leaf texture.
    private static final int MARGIN = 64;

    private static volatile boolean open;
    // Bumped on every close; background work started in an older session gives up.
    private static volatile int session;
    private static volatile int minX, maxX, minZ, maxZ;

    private WorldMapScreenState() {}

    public static boolean isOpen() {
        return open;
    }

    public static int session() {
        return session;
    }

    /**
     * Render thread, every frame. Closing finishes or cancels all outstanding work.
     */
    public static void update(boolean nowOpen) {
        if (open == nowOpen) return;
        open = nowOpen;
        if (!nowOpen) {
            session++;
            AsyncTileBuffers.cancelAll();
            FrameBudgetQueue.drain();
        }
    }

    /**
     * Render thread, after the map screen renders a frame.
     */
    public static void setView(double cameraX, double cameraZ, double halfWidth, double halfHeight) {
        minX = (int) Math.floor(cameraX - halfWidth) - MARGIN;
        maxX = (int) Math.ceil(cameraX + halfWidth) + MARGIN;
        minZ = (int) Math.floor(cameraZ - halfHeight) - MARGIN;
        maxZ = (int) Math.ceil(cameraZ + halfHeight) + MARGIN;
    }

    public static boolean isVisible(int blockX, int blockZ, int size) {
        return blockX + size > minX && blockX <= maxX && blockZ + size > minZ && blockZ <= maxZ;
    }
}
