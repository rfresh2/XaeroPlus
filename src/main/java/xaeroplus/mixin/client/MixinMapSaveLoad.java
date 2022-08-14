package xaeroplus.mixin.client;

import net.minecraft.world.World;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.file.MapRegionInfo;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.RegionDetection;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.region.*;
import xaero.map.world.MapDimension;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mixin(value = MapSaveLoad.class, remap = false)
public abstract class MixinMapSaveLoad {
    private Instant start = Instant.now();

    private ExecutorService regionDetectionExecutor = Executors.newSingleThreadExecutor();

    @Shadow
    private Path lastRealmOwnerPath;
    @Shadow
    private MapProcessor mapProcessor;

    @Shadow
    public abstract Path getCacheFolder(Path subFolder);

    @Inject(method = "detectRegions", at = @At("HEAD"))
    public void detectRegionsHead(CallbackInfo ci) {
        start = Instant.now();

    }

    @Inject(method = "detectRegions", at = @At("TAIL"))
    public void detectRegionsTail(CallbackInfo ci) {
        WorldMap.LOGGER.info("Regions detected in " + (Instant.now().getEpochSecond() - start.getEpochSecond()) + " seconds");
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void detectRegionsFromFiles(MapDimension mapDimension,
                                       String worldId,
                                       String dimId,
                                       String mwId,
                                       Path folder,
                                       String regex,
                                       String splitRegex,
                                       int xIndex,
                                       int zIndex,
                                       int emptySize) {
        // optimizations on top of region detection
        // prefer detecting full regions first
        // load all the cache files in the background
        // this will reduce time for map to appear on screen
        // very noticeable impact when you have a large amount of regions to load
        // this does cause noticeably high IO impact
        final List<RegionDetection> regionDetectionList = new ArrayList<>(1000);
        Pattern p = Pattern.compile(regex);
        try (DirectoryStream<Path> pathStream = Files.newDirectoryStream(folder, (entry -> {
            Matcher m = p.matcher(entry.getFileName().toString());
            return m.matches();
        }))) {
            pathStream.forEach(path -> {
                String regionName = path.getFileName().toString();
                String[] args = regionName.substring(0, regionName.lastIndexOf(46)).split(splitRegex);
                int x = Integer.parseInt(args[xIndex]);
                int z = Integer.parseInt(args[zIndex]);
                RegionDetection regionDetection = new RegionDetection(worldId, dimId, mwId, x, z, path.toFile(), this.mapProcessor.getGlobalVersion(), true);
                regionDetectionList.add(regionDetection);
                this.mapProcessor.addRegionDetection(mapDimension, regionDetection);
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // todo: we don't have a good way to stop this when dimension changes
        // most of the IO is frontloaded so its not the worst, but can be improved.
        regionDetectionExecutor.submit(() -> {
            final Instant before = Instant.now();
            Path cachePath = this.getCacheFolder(folder);
            if (!Files.isDirectory(cachePath)) {
                try {
                    Files.createDirectory(cachePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Map<String, Path> cacheFilesMap = resolveCacheFiles(cachePath);
            regionDetectionList.forEach(regionDetection -> {
                File cacheFile = getCacheFile(regionDetection, cacheFilesMap, cachePath, true);
                regionDetection.setCacheFile(cacheFile);
            });
            final Instant after = Instant.now();
            WorldMap.LOGGER.info("Cache files detected in " + (after.getEpochSecond() - before.getEpochSecond()) + " seconds");
        });
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void updateCacheFolderList(Path subFolder) {
        // kek
        // overwriting this doesn't actually disable caching
        // resolves massive hangs due to terrible original code in this method
    }

    /**
     * @author
     * @reason
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
     * @author
     * @reason
     */
    @Overwrite
    public boolean saveRegion(MapRegion region, int extraAttempts) {
        try {
            if (!region.hasHadTerrain()) {
                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("Save not required for highlight-only region: " + region + " " + region.getWorldId() + " " + region.getDimId());
                }
                return region.countChunks() > 0;
            }
            if (!region.isMultiplayer()) {
                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("Save not required for singleplayer: " + region + " " + region.getWorldId() + " " + region.getDimId());
                }
                return region.countChunks() > 0;
            }
            File permFile = this.getFile(region);
            File tempFile = this.getTempFile(permFile);
            if (tempFile == null) {
                return true;
            }
            if (!tempFile.exists()) {
                tempFile.createNewFile();
            }
            boolean regionIsEmpty = true;

            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                try (FramedLZ4CompressorOutputStream lz4CompressorOutputStream = new FramedLZ4CompressorOutputStream(fileOutputStream))
                {
                    try (DataOutputStream dataOutputStream = new DataOutputStream(lz4CompressorOutputStream)) {
                        dataOutputStream.write(255);
                        dataOutputStream.writeInt(4);
                        for (int o = 0; o < 8; ++o) {
                            for (int p = 0; p < 8; ++p) {
                                BranchLeveledRegion parentRegion;
                                MapTileChunk chunk = region.getChunk(o, p);
                                if (chunk == null) continue;
                                if (chunk.includeInSave()) {
                                    dataOutputStream.write(o << 4 | p);
                                    boolean chunkIsEmpty = true;
                                    for (int i = 0; i < 4; ++i) {
                                        for (int j = 0; j < 4; ++j) {
                                            MapTile tile = chunk.getTile(i, j);
                                            if (tile != null && tile.isLoaded()) {
                                                chunkIsEmpty = false;
                                                for (int x = 0; x < 16; ++x) {
                                                    MapBlock[] c = tile.getBlockColumn(x);
                                                    for (int z = 0; z < 16; ++z) {
                                                        this.savePixel(c[z], dataOutputStream);
                                                    }
                                                }
                                                dataOutputStream.write(tile.getWorldInterpretationVersion());
                                                continue;
                                            }
                                            dataOutputStream.writeInt(-1);
                                        }
                                    }
                                    if (chunkIsEmpty) continue;
                                    regionIsEmpty = false;
                                    continue;
                                }
                                if (!chunk.hasHighlightsIfUndiscovered()) {
                                    region.setChunk(o, p, null);
                                    MapTileChunk chunkIsEmpty = chunk;
                                    synchronized (chunkIsEmpty) {
                                        chunk.getLeafTexture().deleteTexturesAndBuffers();
                                    }
                                }
                                if ((parentRegion = region.getParent()) == null) continue;
                                parentRegion.setShouldCheckForUpdatesRecursive(true);
                            }
                        }
                    }
                }
            }
            if (regionIsEmpty) {
                this.safeDelete(permFile.toPath(), ".xaero");
                this.safeDelete(tempFile.toPath(), ".temp");
                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("Save cancelled because the region is empty: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                }
                return false;
            }
            this.safeMoveAndReplace(tempFile.toPath(), permFile.toPath(), ".xaero.temp", ".xaero");
            if (WorldMap.settings.debug) {
                WorldMap.LOGGER.info("Region saved: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + this.mapProcessor.getMapWriter().getUpdateCounter());
            }
            return true;
        }
        catch (IOException ioe) {
            WorldMap.LOGGER.error("IO exception while trying to save " + region, (Throwable)ioe);
            if (extraAttempts > 0) {
                WorldMap.LOGGER.info("(World Map) Retrying...");
                try {
                    Thread.sleep(20L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                return this.saveRegion(region, extraAttempts - 1);
            }
            return true;
        }
    }

    @Shadow
    public abstract Path getMainFolder(String world, String dim);

    @Shadow
    abstract Path getMWSubFolder(String world, Path mainFolder, String mw);

    /**
     * @author
     * @reason
     */
    @Overwrite
    public File getFile(MapRegion region) {
        if (region.getWorldId() == null) {
            return null;
        }
        File detectedFile = region.getRegionFile();
        boolean realms = this.mapProcessor.isWorldRealms(region.getWorldId());
        boolean multiplayer = region.isMultiplayer();
        if (!multiplayer) {
            if (detectedFile != null) {
                return detectedFile;
            }
            return this.mapProcessor.getWorldDataHandler().getWorldDir().toPath().resolve("region").resolve("r." + region.getRegionX() + "." + region.getRegionZ() + ".mca").toFile();
        }
        Path mainFolder = this.getMainFolder(region.getWorldId(), region.getDimId());
        Path subFolder = this.getMWSubFolder(region.getWorldId(), mainFolder, region.getMwId());
        try {
            File subFolderFile = subFolder.toFile();
            if (!subFolderFile.exists()) {
                Path ownerPath;
                Files.createDirectories(subFolderFile.toPath());
                if (realms && WorldMap.events.getLatestRealm() != null && !(ownerPath = mainFolder.resolve(WorldMap.events.getLatestRealm().owner + ".owner")).equals(this.lastRealmOwnerPath)) {
                    if (!Files.exists(ownerPath)) {
                        Files.createFile(ownerPath);
                    }
                    this.lastRealmOwnerPath = ownerPath;
                }
            }
        }
        catch (IOException e1) {
            WorldMap.LOGGER.error("suppressed exception", (Throwable)e1);
        }
        if (detectedFile != null && detectedFile.getName().endsWith(".xaero")) {
            return detectedFile;
        }
        return detectedFile == null ? subFolder.resolve(region.getRegionX() + "_" + region.getRegionZ() + ".xaero").toFile() : detectedFile;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public File getTempFile(File realFile) {
        return this.getSecondaryFile(".xaero.temp", realFile);
    }

    @Shadow
    protected abstract void savePixel(MapBlock pixel, DataOutputStream out) throws IOException;

    @Shadow
    protected abstract File getSecondaryFile(String extension, File realFile);

    @Shadow
    public abstract void safeDelete(Path filePath, String extension) throws IOException;

    @Shadow
    public abstract void safeMoveAndReplace(Path fromPath, Path toPath, String fromExtension, String toExtension) throws IOException;

    @Shadow
    public abstract void backupFile(File file, int saveVersion) throws IOException;

    @Shadow
    protected abstract void loadPixel(Integer next, MapBlock pixel, DataInputStream in, int saveVersion, World world, int[] biomeBuffer, BlockStateColorTypeCache colorTypeCache) throws IOException;

    @Shadow
    private BlockStateShortShapeCache blockStateShortShapeCache;

    /**
         * @author
         * @reason
         */
    @Overwrite
    public boolean loadRegion(World world, MapRegion region, BlockStateColorTypeCache colourTypeCache, int extraAttempts) {
        boolean multiplayer = region.isMultiplayer();
        File file = this.getFile(region);
        if (!region.hasHadTerrain() || file == null || !file.exists()) {
            if (region.getLoadState() == 4) {
                region.setSaveExists(null);
            }
            if (region.hasHadTerrain()) {
                return false;
            }
            MapRegion mapRegion = region;
            synchronized (mapRegion) {
                region.setLoadState((byte)1);
            }
            region.restoreBufferUpdateObjects();
            if (WorldMap.settings.debug) {
                WorldMap.LOGGER.info("Highlight region fake-loaded: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
            }
            return true;
        }
        int saveVersion = -1;
        boolean versionReached = false;
        int[] biomeBuffer = new int[3];
        try {
            boolean result;
            MapRegion mapRegion = region;
            synchronized (mapRegion) {
                region.setLoadState((byte)1);
            }
            region.setSaveExists(true);
            region.restoreBufferUpdateObjects();
            int totalChunks = 0;
            if (multiplayer) {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    try (FramedLZ4CompressorInputStream framedLZ4CompressorInputStream = new FramedLZ4CompressorInputStream(fileInputStream)) {
                        try (DataInputStream dataInputStream = new DataInputStream(framedLZ4CompressorInputStream)) {
                            int firstByte = dataInputStream.read();
                            if (firstByte == 255) {
                                saveVersion = dataInputStream.readInt();
                                if (4 < saveVersion) {
                                    WorldMap.LOGGER.info("Trying to load a newer region " + region + " save using an older version of Xaero's World Map!");
                                    this.backupFile(file, saveVersion);
                                    region.setSaveExists(null);
                                    return false;
                                }
                                firstByte = -1;
                            }
                            versionReached = true;
                            LeveledRegion leveledRegion = region.getLevel() == 3 ? region : region.getParent();
                            synchronized (leveledRegion) {
                                MapRegion mapRegion2 = region;
                                synchronized (mapRegion2) {
                                    for (int o = 0; o < 8; ++o) {
                                        for (int p = 0; p < 8; ++p) {
                                            MapTileChunk chunk2 = region.getChunk(o, p);
                                            if (chunk2 == null) continue;
                                            chunk2.setLoadState((byte)1);
                                        }
                                    }
                                }
                            }
                            while (true) {
                                int n;
                                int n2 = n = firstByte == -1 ? dataInputStream.read() : firstByte;
                                if (n == -1) break;
                                firstByte = -1;
                                int o = n >> 4;
                                int p = n & 0xF;
                                MapTileChunk chunk = region.getChunk(o, p);
                                if (chunk == null) {
                                    chunk = new MapTileChunk(region, region.getRegionX() * 8 + o, region.getRegionZ() * 8 + p);
                                    region.setChunk(o, p, chunk);
                                    MapRegion chunk2 = region;
                                    synchronized (chunk2) {
                                        region.setAllCachePrepared(false);
                                    }
                                }
                                if (region.isMetaLoaded()) {
                                    chunk.getLeafTexture().setBufferedTextureVersion(region.getAndResetCachedTextureVersion(o, p));
                                }
                                chunk.resetHeights();
                                for (int i = 0; i < 4; ++i) {
                                    for (int j = 0; j < 4; ++j) {
                                        Integer nextTile = dataInputStream.readInt();
                                        if (nextTile == -1) continue;
                                        MapTile tile = this.mapProcessor.getTilePool().get(this.mapProcessor.getCurrentDimension(), chunk.getX() * 4 + i, chunk.getZ() * 4 + j);
                                        for (int x = 0; x < 16; ++x) {
                                            MapBlock[] c = tile.getBlockColumn(x);
                                            for (int z = 0; z < 16; ++z) {
                                                if (c[z] == null) {
                                                    c[z] = new MapBlock();
                                                } else {
                                                    c[z].prepareForWriting();
                                                }
                                                this.loadPixel(nextTile, c[z], dataInputStream, saveVersion, world, biomeBuffer, colourTypeCache);
                                                nextTile = null;
                                            }
                                        }
                                        if (saveVersion >= 4) {
                                            tile.setWorldInterpretationVersion(dataInputStream.read());
                                        }
                                        chunk.setTile(i, j, tile, this.blockStateShortShapeCache);
                                        tile.setLoaded(true);
                                    }
                                }
                                if (!chunk.includeInSave()) {
                                    if (chunk.hasHighlightsIfUndiscovered()) continue;
                                    region.setChunk(o, p, null);
                                    chunk.getLeafTexture().deleteTexturesAndBuffers();
                                    chunk = null;
                                    continue;
                                }
                                region.pushWriterPause();
                                ++totalChunks;
                                chunk.setToUpdateBuffers(true);
                                chunk.setLoadState((byte)2);
                                region.popWriterPause();
                            }
                        }
                    }
                }
                if (totalChunks > 0) {
                    if (WorldMap.settings.debug) {
                        WorldMap.LOGGER.info("Region loaded: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + saveVersion);
                    }
                    return true;
                }
                region.setSaveExists(null);
                this.safeDelete(file.toPath(), ".xaero");
                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("Cancelled loading an empty region: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " + saveVersion);
                }
                return false;
            }
            int[] chunkCount = new int[1];
            WorldDataHandler.Result buildResult = this.mapProcessor.getWorldDataHandler().buildRegion(region, world, true, chunkCount);
            if (buildResult == WorldDataHandler.Result.CANCEL) {
                if (region.hasHadTerrain()) {
                    RegionDetection restoredDetection = new RegionDetection(region.getWorldId(), region.getDimId(), region.getMwId(), region.getRegionX(), region.getRegionZ(), region.getRegionFile(), this.mapProcessor.getGlobalVersion(), true);
                    restoredDetection.transferInfoFrom(region);
                    this.mapProcessor.addRegionDetection(region.getDim(), restoredDetection);
                }
                this.mapProcessor.removeMapRegion(region);
                WorldMap.LOGGER.info("Region cancelled from world save: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                return false;
            }
            region.setRegionFile(file);
            boolean bl = result = buildResult == WorldDataHandler.Result.SUCCESS && chunkCount[0] > 0;
            if (!result) {
                region.setSaveExists(null);
                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("Region failed to load from world save: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
                }
            } else if (WorldMap.settings.debug) {
                WorldMap.LOGGER.info("Region loaded from world save: " + region + " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId());
            }
            return result;
        }
        catch (IOException ioe) {
            WorldMap.LOGGER.error("IO exception while trying to load " + region, (Throwable)ioe);
            if (extraAttempts > 0) {
                MapRegion mapRegion = region;
                synchronized (mapRegion) {
                    region.setLoadState((byte)4);
                }
                WorldMap.LOGGER.info("(World Map) Retrying...");
                try {
                    Thread.sleep(20L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                return this.loadRegion(world, region, colourTypeCache, extraAttempts - 1);
            }
            region.setSaveExists(null);
            return false;
        }
        catch (Throwable e) {
            region.setSaveExists(null);
            WorldMap.LOGGER.error("Region failed to load: " + region + (versionReached ? " " + saveVersion : ""), e);
            return false;
        }
    }

    // mfw finding cache files is slower than just reading the region
    // only really a problem when cache is VERY populated. Mine has 6k entries
    // this is still very IO intensive
    Map<String, Path> resolveCacheFiles(final Path cacheDirectory) {
        try {
            return Files.list(cacheDirectory)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        return filename.endsWith(".xwmc") || filename.endsWith(".xwmc.outdated");
                    })
                    .collect(Collectors.toMap(k -> k.getFileName().toString(), v -> v));
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public File getCacheFile(MapRegionInfo region, Map<String, Path> cacheFiles, Path cacheFilesPath, final boolean requestCache) {
        final String expectedCacheFileName = region.getRegionX() + "_" + region.getRegionZ() + ".xwmc";
        if (cacheFiles.containsKey(expectedCacheFileName)) {
            return cacheFiles.get(expectedCacheFileName).toFile();
        }
        if (requestCache) {
            region.setShouldCache(true, "cache file");
        }
        if (cacheFiles.containsKey(expectedCacheFileName + ".outdated")) {
            return cacheFiles.get(expectedCacheFileName + ".outdated").toFile();
        } else {
            return cacheFilesPath.resolve(region.getRegionX() + "_" + region.getRegionZ() + ".xwmc").toFile();
        }
    }
}
