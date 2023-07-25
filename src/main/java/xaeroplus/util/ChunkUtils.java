package xaeroplus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.ChunkPos;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class ChunkUtils {

    /** Caching helpers **/
    public static long chunkPosToLong(final ChunkPos chunkPos) {
        return (long)chunkPos.x & 4294967295L | ((long)chunkPos.z & 4294967295L) << 32;
    }

    public static long chunkPosToLong(final int x, final int z) {
        return (long)x & 4294967295L | ((long)z & 4294967295L) << 32;
    }

    public static ChunkPos longToChunkPos(final long l) {
        return new ChunkPos((int)(l & 4294967295L), (int)(l >> 32 & 4294967295L));
    }

    public static int longToChunkX(final long l) {
        return (int)(l & 4294967295L);
    }

    public static int longToChunkZ(final long l) {
        return (int)(l >> 32 & 4294967295L);
    }
    public static int posToChunkPos(final int i) {
        return i >> 4;
    }

    public static Callable<List<HighlightAtChunkPos>> loadHighlightChunksAtRegion(
            final int leafRegionX, final int leafRegionZ, final int level,
            final Function<Long, Boolean> highlightChunkPosFunction) {
        return () -> {
            final List<HighlightAtChunkPos> chunks = new ArrayList<>();
            final int mx = leafRegionX + level;
            final int mz = leafRegionZ + level;
            for (int regX = leafRegionX; regX < mx; ++regX) {
                for (int regZ = leafRegionZ; regZ < mz; ++regZ) {
                    for (int cx = 0; cx < 8; cx++) {
                        for (int cz = 0; cz < 8; cz++) {
                            final int mapTileChunkX = (regX << 3) + cx;
                            final int mapTileChunkZ = (regZ << 3) + cz;
                            for (int t = 0; t < 16; ++t) {
                                final int chunkPosX = (mapTileChunkX << 2) + t % 4;
                                final int chunkPosZ = (mapTileChunkZ << 2) + (t >> 2);
                                if (highlightChunkPosFunction.apply(ChunkUtils.chunkPosToLong(chunkPosX, chunkPosZ))) {
                                    chunks.add(new HighlightAtChunkPos(chunkPosX, chunkPosZ));
                                }
                            }
                        }
                    }
                }
            }
            return chunks;
        };
    }

    /** Player position util functions **/
    public static int actualPlayerChunkX() {
        try {
            return Minecraft.getMinecraft().player.chunkCoordX;
        } catch (final NullPointerException e) {
            return 0;
        }
    }
    public static int getPlayerChunkX() {
        return coordToChunkCoord(getPlayerX());
    }
    public static int actualPlayerChunkZ() {
        try {
            return Minecraft.getMinecraft().player.chunkCoordZ;
        } catch (final NullPointerException e) {
            return 0;
        }
    }
    public static int getPlayerChunkZ() {
        return coordToChunkCoord(getPlayerZ());
    }
    public static int actualPlayerRegionX() {
        return actualPlayerChunkX() >> 5;
    }
    public static int getPlayerRegionX() {
        return getPlayerChunkX() >> 5;
    }
    public static int actualPlayerRegionZ() {
        return actualPlayerChunkZ() >> 5;
    }
    public static int getPlayerRegionZ() {
        return getPlayerChunkZ() >> 5;
    }
    public static double getPlayerX() {
        try {
            final Minecraft mc = Minecraft.getMinecraft();
            int dim = mc.world.provider.getDimension();
            // when player is in the nether or the custom dimension is the nether, perform coordinate translation
            if ((dim == -1 || Shared.customDimensionId == -1) && dim != Shared.customDimensionId) {
                if (Shared.customDimensionId == 0) {
                    return mc.player.posX * 8.0;
                } else if (Shared.customDimensionId == -1 && dim == 0) {
                    return mc.player.posX / 8.0;
                }
            }
            return mc.player.posX;
        } catch (final Exception e) {
            return 0;
        }
    }
    public static double actualPlayerX() {
        try {
            return Minecraft.getMinecraft().player.posX;
        } catch (final Exception e) {
            return 0;
        }
    }
    public static double getPlayerZ() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            int dim = mc.world.provider.getDimension();
            // when player is in the nether or the custom dimension is the nether, perform coordinate translation
            if ((dim == -1 || Shared.customDimensionId == -1) && dim != Shared.customDimensionId) {
                if (Shared.customDimensionId == 0) {
                    return mc.player.posZ * 8.0;
                } else if (Shared.customDimensionId == -1 && dim == 0) {
                    return mc.player.posZ / 8.0;
                }
            }
            return mc.player.posZ;
        } catch (final Exception e) {
            return 0;
        }
    }
    public static double actualPlayerZ() {
        try {
            return Minecraft.getMinecraft().player.posZ;
        } catch (final Exception e) {
            return 0;
        }
    }

    public static int getActualDimension() {
        try {
            return Minecraft.getMinecraft().world.provider.getDimension();
        } catch (final Exception e) {
            return 0;
        }
    }

    /** MC Coordinate conversion functions **/

    public static int coordToChunkCoord(final double coord) {
        return ((int)coord) >> 4;
    }
    public static int coordToRegionCoord(final double coord) {
        return ((int)coord) >> 9;
    }
    public static int chunkCoordToCoord(final int chunkCoord) {
        return chunkCoord << 4;
    }
    public static int chunkCoordToRegionCoord(final int chunkCoord) {
        return chunkCoord >> 5;
    }
    public static int regionCoordToChunkCoord(final int regionCoord) {
        return regionCoord << 5;
    }
    public static int regionCoordToCoord(final int regionCoord) {
        return regionCoord << 9;
    }

    /** Xaero coordinate conversion functions **/

    /**
     * MCRegion Format:
     *   Region = 32x32 Chunks
     *   Chunk = 16x16 Blocks
     *
     * XaeroRegion Format:
     *   MapRegion = 8x8 MapTileChunks
     *   MapTileChunk = 4x4 Tiles
     *   Tile = 16x16 Blocks
     */

    public static int coordToMapRegionCoord(final int coord) {
        return coord >> 9;
    }
    public static int mapRegionCoordToCoord(final int mapRegionCoord) {
        return mapRegionCoord << 9;
    }
    public static int mapTileChunkCoordToMapRegionCoord(final int mapTileChunkCoord) {
        return mapTileChunkCoord >> 3;
    }
    public static int mapRegionCoordToMapTileChunkCoord(final int mapRegionCoord) {
        return mapRegionCoord << 3;
    }
    public static int mapTileCoordToMapTileChunkCoord(final int mapTileCoord) {
        return mapTileCoord >> 2;
    }
    public static int mapTileChunkCoordToMapTileCoord(final int mapTileChunkCoord) {
        return mapTileChunkCoord << 2;
    }
    public static int mapTileCoordToCoord(final int mapTileCoord) {
        return mapTileCoord << 4;
    }
    public static int coordToMapTileCoord(final int coord) {
        return coord >> 4;
    }
    public static int mapTileCoordToMapRegionCoord(final int mapTileCoord) {
        return mapTileCoord >> 6;
    }
    public static int mapRegionCoordToMapTileCoord(final int mapRegionCoord) {
        return mapRegionCoord << 6;
    }
    public static int mapTileChunkCoordToCoord(final int mapTileChunkCoord) {
        return mapTileChunkCoord << 6;
    }
    public static int coordToMapTileChunkCoord(final int coord) {
        return coord >> 6;
    }
    public static int chunkCoordToMapRegionCoord(final int chunkCoord) {
        return chunkCoord >> 5;
    }
    public static int mapRegionCoordToChunkCoord(final int mapRegionCoord) {
        return mapRegionCoord << 5;
    }
    public static int chunkCoordToMapTileChunkCoord(final int chunkCoord) {
        return chunkCoord >> 2;
    }
    public static int mapTileChunkCoordToChunkCoord(final int mapTileChunkCoord) {
        return mapTileChunkCoord << 2;
    }
    public static int chunkCoordToMapTileCoord(final int chunkCoord) {
        return chunkCoord;
    }
    public static int mapTileCoordToChunkCoord(final int mapTileCoord) {
        return mapTileCoord;
    }
    public static int regionCoordToMapRegionCoord(final int regionCoord) {
        return regionCoord;
    }
    public static int mapRegionCoordToRegionCoord(final int mapRegionCoord) {
        return mapRegionCoord;
    }
    public static int regionCoordToMapTileChunkCoord(final int regionCoord) {
        return regionCoord << 3;
    }
    public static int mapTileChunkCoordToRegionCoord(final int mapTileChunkCoord) {
        return mapTileChunkCoord >> 3;
    }
    public static int regionCoordToMapTileCoord(final int regionCoord) {
        return regionCoord << 6;
    }
    public static int mapTileCoordToRegionCoord(final int mapTileCoord) {
        return mapTileCoord >> 6;
    }
    public static int chunkCoordToMapTileChunkCoordLocal(final int chunkCoord) {
        return chunkCoordToMapTileChunkCoord(chunkCoord) & 7;
    }
    public static int chunkCoordToMapTileCoordLocal(final int chunkCoord) {
        return chunkCoordToMapTileCoord(chunkCoord) & 3;
    }
}
