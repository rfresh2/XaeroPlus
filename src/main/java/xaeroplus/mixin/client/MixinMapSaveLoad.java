package xaeroplus.mixin.client;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.RegionDetection;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.region.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

import static xaeroplus.XaeroPlus.MAX_LEVEL;

@Mixin(value = MapSaveLoad.class, remap = false)
public abstract class MixinMapSaveLoad {
    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    public abstract File getFile(MapRegion region);

    @Shadow
    public abstract void backupFile(File file, int saveVersion);
    @Shadow
    private BlockStateShortShapeCache blockStateShortShapeCache;
    @Shadow
    protected abstract void loadPixel(Integer next, MapBlock pixel, DataInputStream in, int saveVersion, World world, int[] biomeBuffer, BlockStateColorTypeCache colorTypeCache) throws IOException;
    @Shadow
    public abstract void safeDelete(Path filePath, String extension);

    /**
     * @author rfresh2
     * @reason Use DIM0 as overworld dimension directory instead of "null"
     */
    @Overwrite
    public Path getOldFolder(String oldUnfixedMainId, String dim) {
        if (oldUnfixedMainId == null) {
            return null;
        }
        String dimIdFixed = dim.equals("null") ? "0" : dim;
        return WorldMap.saveFolder.toPath().resolve(oldUnfixedMainId + "_" + dimIdFixed);
    }

    /**
     * @author rfresh2
     * @reason Replace hardcoded cache level with variable
     */
    @Overwrite
    public boolean loadRegion(World world, MapRegion region, BlockStateColorTypeCache colourTypeCache, int extraAttempts) {
        boolean multiplayer = region.isMultiplayer();
        int emptySize = multiplayer ? 0 : 8192;
        int saveVersion = -1;
        boolean versionReached = false;
        int[] biomeBuffer = new int[3];

        try {
            File file = this.getFile(region);
            if (region.hasHadTerrain() && file != null && file.exists() && Files.size(file.toPath()) > (long) emptySize) {
                synchronized (region) {
                    region.setLoadState((byte) 1);
                }

                region.setSaveExists(true);
                region.restoreBufferUpdateObjects();
                int totalChunks = 0;
                if (multiplayer) {
                    DataInputStream in = null;

                    try {
                        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(file), 2048));
                        in = new DataInputStream(zipIn);
                        zipIn.getNextEntry();
                        int firstByte = in.read();
                        if (firstByte == 255) {
                            saveVersion = in.readInt();
                            if (4 < saveVersion) {
                                zipIn.closeEntry();
                                in.close();
                                WorldMap.LOGGER.info("Trying to load a newer region " + region + " save using an older version of Xaero's World Map!");
                                this.backupFile(file, saveVersion);
                                region.setSaveExists(null);
                                return false;
                            }

                            firstByte = -1;
                        }

                        versionReached = true;
                        synchronized (region.getLevel() == MAX_LEVEL ? region : region.getParent()) { // replace hardcoded max level
                            synchronized (region) {
                                for (int o = 0; o < 8; ++o) {
                                    for (int p = 0; p < 8; ++p) {
                                        MapTileChunk chunk = region.getChunk(o, p);
                                        if (chunk != null) {
                                            chunk.setLoadState((byte) 1);
                                        }
                                    }
                                }
                            }
                        }

                        while (true) {
                            int chunkCoords = firstByte == -1 ? in.read() : firstByte;
                            if (chunkCoords == -1) {
                                zipIn.closeEntry();
                                break;
                            }

                            firstByte = -1;
                            int o = chunkCoords >> 4;
                            int p = chunkCoords & 15;
                            MapTileChunk chunk = region.getChunk(o, p);
                            if (chunk == null) {
                                region.setChunk(o, p, chunk = new MapTileChunk(region, region.getRegionX() * 8 + o, region.getRegionZ() * 8 + p));
                                synchronized (region) {
                                    region.setAllCachePrepared(false);
                                }
                            }

                            if (region.isMetaLoaded()) {
                                chunk.getLeafTexture().setBufferedTextureVersion(region.getAndResetCachedTextureVersion(o, p));
                            }

                            chunk.resetHeights();

                            for (int i = 0; i < 4; ++i) {
                                for (int j = 0; j < 4; ++j) {
                                    Integer nextTile = in.readInt();
                                    if (nextTile != -1) {
                                        MapTile tile = this.mapProcessor
                                                .getTilePool()
                                                .get(this.mapProcessor.getCurrentDimension(), chunk.getX() * 4 + i, chunk.getZ() * 4 + j);

                                        for (int x = 0; x < 16; ++x) {
                                            MapBlock[] c = tile.getBlockColumn(x);

                                            for (int z = 0; z < 16; ++z) {
                                                if (c[z] == null) {
                                                    c[z] = new MapBlock();
                                                } else {
                                                    c[z].prepareForWriting();
                                                }

                                                this.loadPixel(nextTile, c[z], in, saveVersion, world, biomeBuffer, colourTypeCache);
                                                nextTile = null;
                                            }
                                        }

                                        if (saveVersion >= 4) {
                                            tile.setWorldInterpretationVersion(in.read());
                                        }

                                        chunk.setTile(i, j, tile, this.blockStateShortShapeCache);
                                        tile.setLoaded(true);
                                    }
                                }
                            }

                            if (!chunk.includeInSave()) {
                                if (!chunk.hasHighlightsIfUndiscovered()) {
                                    region.setChunk(o, p, null);
                                    chunk.getLeafTexture().deleteTexturesAndBuffers();
                                    MapTileChunk var56 = null;
                                }
                            } else {
                                region.pushWriterPause();
                                ++totalChunks;
                                chunk.setToUpdateBuffers(true);
                                chunk.setLoadState((byte) 2);
                                region.popWriterPause();
                            }
                        }
                    } finally {
                        if (in != null) {
                            in.close();
                        }

                    }

                    if (totalChunks > 0) {
                        if (WorldMap.settings.debug) {
                            WorldMap.LOGGER
                                    .info("Region loaded: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + saveVersion);
                        }

                        return true;
                    } else {
                        region.setSaveExists(null);
                        this.safeDelete(file.toPath(), ".zip");
                        if (WorldMap.settings.debug) {
                            WorldMap.LOGGER
                                    .info(
                                            "Cancelled loading an empty region: "
                                                    + region
                                                    + " "
                                                    + region.getWorldId()
                                                    + " "
                                                    + region.getDimId()
                                                    + " "
                                                    + region.getMwId()
                                                    + ", "
                                                    + saveVersion
                                    );
                        }

                        return false;
                    }
                } else {
                    int[] chunkCount = new int[1];
                    WorldDataHandler.Result buildResult = this.mapProcessor.getWorldDataHandler().buildRegion(region, world, true, chunkCount);
                    if (buildResult == WorldDataHandler.Result.CANCEL) {
                        if (region.hasHadTerrain()) {
                            RegionDetection restoredDetection = new RegionDetection(
                                    region.getWorldId(),
                                    region.getDimId(),
                                    region.getMwId(),
                                    region.getRegionX(),
                                    region.getRegionZ(),
                                    region.getRegionFile(),
                                    this.mapProcessor.getGlobalVersion(),
                                    true
                            );
                            restoredDetection.transferInfoFrom(region);
                            this.mapProcessor.addRegionDetection(region.getDim(), restoredDetection);
                        }

                        this.mapProcessor.removeMapRegion(region);
                        WorldMap.LOGGER
                                .info("Region cancelled from world save: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                        return false;
                    } else {
                        region.setRegionFile(file);
                        boolean result = buildResult == WorldDataHandler.Result.SUCCESS && chunkCount[0] > 0;
                        if (!result) {
                            region.setSaveExists(null);
                            if (WorldMap.settings.debug) {
                                WorldMap.LOGGER
                                        .info(
                                                "Region failed to load from world save: "
                                                        + region
                                                        + " "
                                                        + region.getWorldId()
                                                        + " "
                                                        + region.getDimId()
                                                        + " "
                                                        + region.getMwId()
                                        );
                            }
                        } else if (WorldMap.settings.debug) {
                            WorldMap.LOGGER
                                    .info("Region loaded from world save: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                        }

                        return result;
                    }
                }
            } else {
                if (region.getLoadState() == 4) {
                    region.setSaveExists(null);
                }

                if (region.hasHadTerrain()) {
                    return false;
                } else {
                    synchronized (region) {
                        region.setLoadState((byte) 1);
                    }

                    region.restoreBufferUpdateObjects();
                    if (WorldMap.settings.debug) {
                        WorldMap.LOGGER
                                .info("Highlight region fake-loaded: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                    }

                    return true;
                }
            }
        } catch (IOException var46) {
            WorldMap.LOGGER.error("IO exception while trying to load " + region, var46);
            if (extraAttempts > 0) {
                synchronized (region) {
                    region.setLoadState((byte) 4);
                }

                WorldMap.LOGGER.info("(World Map) Retrying...");

                try {
                    Thread.sleep(20L);
                } catch (InterruptedException var38) {
                }

                return this.loadRegion(world, region, colourTypeCache, extraAttempts - 1);
            } else {
                region.setSaveExists(null);
                return false;
            }
        } catch (Throwable var47) {
            region.setSaveExists(null);
            WorldMap.LOGGER.error("Region failed to load: " + region + (versionReached ? " " + saveVersion : ""), var47);
            return false;
        }
    }
}
