package xaeroplus.mixin.client;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.RegionDetection;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.region.*;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.util.CustomDimensionMapSaveLoad;
import xaeroplus.util.Shared;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static xaeroplus.util.Shared.decompressZipToBytes;

@Mixin(value = MapSaveLoad.class, remap = false)
public abstract class MixinMapSaveLoad implements CustomDimensionMapSaveLoad {
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
    @Shadow
    public abstract File getTempFile(File realFile);
    @Shadow
    protected abstract void savePixel(MapBlock pixel, DataOutputStream out);
    @Shadow
    public abstract void safeMoveAndReplace(Path fromPath, Path toPath, String fromExtension, String toExtension);
    @Shadow
    public abstract Path getMWSubFolder(String world, String dim, String mw);
    @Shadow
    public abstract void detectRegionsFromFiles(
            MapDimension mapDimension,
            String worldId,
            String dimId,
            String mwId,
            Path folder,
            String regex,
            int xIndex,
            int zIndex,
            int emptySize,
            int attempts,
            Consumer<RegionDetection> detectionConsumer
    );

    @Inject(method = "getOldFolder", at = @At(value = "HEAD"), cancellable = true)
    public void getOldFolder(final String oldUnfixedMainId, final String dim, final CallbackInfoReturnable<Path> cir) {
        if (!Shared.nullOverworldDimensionFolder) {
            if (oldUnfixedMainId == null) {
                cir.setReturnValue(null);
            }
            String dimIdFixed = Objects.equals(dim, "null") ? "0" : dim;
            cir.setReturnValue(WorldMap.saveFolder.toPath().resolve(oldUnfixedMainId + "_" + dimIdFixed));
        }
    }

    /**
     * @author rfresh2
     * @reason faster zip writes
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

                    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(decompressZipToBytes(file.toPath())))) {
                        // fast zip
                        int firstByte = in.read();
                        if (firstByte == 255) {
                            saveVersion = in.readInt();
                            if (7 < saveVersion) {
                                WorldMap.LOGGER.info("Trying to load a newer region " + region + " save using an older version of Xaero's World Map!");
                                this.backupFile(file, saveVersion);
                                region.setSaveExists(null);
                                return false;
                            }

                            firstByte = -1;
                        }

                        versionReached = true;
                        synchronized (region.getLevel() == 3 ? region : region.getParent()) {
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
                                break;
                            }

                            firstByte = -1;
                            int o = chunkCoords >> 4;
                            int p = chunkCoords & 15;
                            MapTileChunk chunk = region.getChunk(o, p);
                            if (chunk == null) {
                                region.setChunk(o, p, chunk = new MapTileChunk(region, (region.getRegionX() << 3) + o, (region.getRegionZ() << 3) + p));
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
                                                .get(this.mapProcessor.getDimensionName(Shared.customDimensionId), chunk.getX() * 4 + i, chunk.getZ() * 4 + j);

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

                                        if (saveVersion >= 6) {
                                            tile.setWrittenCave(in.readInt(), saveVersion >= 7 ? in.read() : 32);
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
                                }
                            } else {
                                region.pushWriterPause();
                                ++totalChunks;
                                chunk.setToUpdateBuffers(true);
                                chunk.setLoadState((byte) 2);
                                region.popWriterPause();
                            }
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
                            region.getDim().getLayeredMapRegions().getLayer(region.getCaveLayer()).addRegionDetection(restoredDetection);
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
                if (region.getLoadState() == 4 || region.hasHadTerrain()) {
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
        } catch (IOException var44) {
            WorldMap.LOGGER.error("IO exception while trying to load " + region, var44);
            if (extraAttempts > 0) {
                synchronized (region) {
                    region.setLoadState((byte) 4);
                }

                WorldMap.LOGGER.info("(World Map) Retrying...");

                try {
                    Thread.sleep(20L);
                } catch (InterruptedException var37) {
                }

                return this.loadRegion(world, region, colourTypeCache, extraAttempts - 1);
            } else {
                region.setSaveExists(null);
                return false;
            }
        } catch (Throwable var45) {
            region.setSaveExists(null);
            WorldMap.LOGGER.error("Region failed to load: " + region + (versionReached ? " " + saveVersion : ""), var45);
            return false;
        }
    }

    /**
     * @author rfresh2
     * @reason zip fast
     */
    @Overwrite
    public boolean saveRegion(MapRegion region, int extraAttempts) {
        try {
            if (!region.hasHadTerrain()) {
                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("Save not required for highlight-only region: " + region + " " + region.getWorldId() + " " + region.getDimId());
                }

                return region.countChunks() > 0;
            } else if (!region.isMultiplayer()) {
                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("Save not required for singleplayer: " + region + " " + region.getWorldId() + " " + region.getDimId());
                }

                return region.countChunks() > 0;
            } else {
                File permFile = this.getFile(region);
                File file = this.getTempFile(permFile);
                if (file == null) {
                    return true;
                } else {
                    if (!file.exists()) {
                        if (!file.createNewFile()) {
                            throw new IOException("Failed to create temp region file: " + file);
                        }
                    }

                    boolean hasAnything = false;
                    boolean regionWasSavedEmpty = true;
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); // does not need to be closed

                    try(DataOutputStream out = new DataOutputStream(byteOut);
                        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
                        //zip fast
                        ZipEntry e = new ZipEntry("region.xaero");
                        zipOut.putNextEntry(e);
                        out.write(255);
                        out.writeInt(7);

                        for(int o = 0; o < 8; ++o) {
                            for(int p = 0; p < 8; ++p) {
                                MapTileChunk chunk = region.getChunk(o, p);
                                if (chunk != null) {
                                    hasAnything = true;
                                    if (!chunk.includeInSave()) {
                                        if (!chunk.hasHighlightsIfUndiscovered()) {
                                            region.setChunk(o, p, null);
                                            synchronized(chunk) {
                                                chunk.getLeafTexture().deleteTexturesAndBuffers();
                                            }
                                        }

                                        BranchLeveledRegion parentRegion = region.getParent();
                                        if (parentRegion != null) {
                                            parentRegion.setShouldCheckForUpdatesRecursive(true);
                                        }
                                    } else {
                                        out.write(o << 4 | p);
                                        boolean chunkIsEmpty = true;

                                        for(int i = 0; i < 4; ++i) {
                                            for(int j = 0; j < 4; ++j) {
                                                MapTile tile = chunk.getTile(i, j);
                                                if (tile != null && tile.isLoaded()) {
                                                    chunkIsEmpty = false;

                                                    for(int x = 0; x < 16; ++x) {
                                                        MapBlock[] c = tile.getBlockColumn(x);

                                                        for(int z = 0; z < 16; ++z) {
                                                            this.savePixel(c[z], out);
                                                        }
                                                    }

                                                    out.write(tile.getWorldInterpretationVersion());
                                                    out.writeInt(tile.getWrittenCaveStart());
                                                    out.write(tile.getWrittenCaveDepth());
                                                } else {
                                                    out.writeInt(-1);
                                                }
                                            }
                                        }

                                        if (!chunkIsEmpty) {
                                            regionWasSavedEmpty = false;
                                        }
                                    }
                                }
                            }
                        }
                        zipOut.write(byteOut.toByteArray());
                        zipOut.closeEntry();
                    }

                    if (regionWasSavedEmpty) {
                        this.safeDelete(permFile.toPath(), ".zip");
                        this.safeDelete(file.toPath(), ".temp");
                        if (WorldMap.settings.debug) {
                            WorldMap.LOGGER
                                    .info(
                                            "Save cancelled because the region would be saved empty: "
                                                    + region
                                                    + " "
                                                    + region.getWorldId()
                                                    + " "
                                                    + region.getDimId()
                                                    + " "
                                                    + region.getMwId()
                                    );
                        }

                        return hasAnything;
                    } else {
                        this.safeMoveAndReplace(file.toPath(), permFile.toPath(), ".temp", ".zip");
                        if (WorldMap.settings.debug) {
                            WorldMap.LOGGER
                                    .info(
                                            "Region saved: "
                                                    + region
                                                    + " "
                                                    + region.getWorldId()
                                                    + " "
                                                    + region.getDimId()
                                                    + " "
                                                    + region.getMwId()
                                                    + ", "
                                                    + this.mapProcessor.getMapWriter().getUpdateCounter()
                                    );
                        }

                        return true;
                    }
                }
            }
        } catch (IOException var28) {
            WorldMap.LOGGER.error("IO exception while trying to save " + region, var28);
            if (extraAttempts > 0) {
                WorldMap.LOGGER.info("(World Map) Retrying...");

                try {
                    Thread.sleep(20L);
                } catch (InterruptedException var25) {
                }

                return this.saveRegion(region, extraAttempts - 1);
            } else {
                return true;
            }
        }
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lxaero/map/region/LayeredRegionManager;addLoadedRegion(Lxaero/map/region/LeveledRegion;)V"))
    public void redirectAddLoadedRegionDimension(LayeredRegionManager instance, LeveledRegion<?> reg) {
        reg.getDim().getLayeredMapRegions().addLoadedRegion(reg);
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;"))
    public MapDimension redirectGetCurrentDimension(MapWorld instance) {
        return instance.getDimension(Shared.customDimensionId);
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lxaero/map/region/LeveledRegion;isAllCachePrepared()Z", ordinal = 0))
    public boolean redirectCacheSaveFailCrash(final LeveledRegion instance) {
        final boolean value = instance.isAllCachePrepared();
        if (!value) {
            XaeroPlus.LOGGER.warn("LeveledRegion cache not prepared. Attempting to repair crash");
            instance.setRecacheHasBeenRequested(false, "crash fix");
            // See MixinMapProcessor for where we catch the exception (which is still thrown)
        }
        return value;
    }

    @Override
    public void detectRegionsInDimension(int attempts, final int dimId) {
        final MapDimension mapDimension = this.mapProcessor.getMapWorld().getDimension(dimId);
        mapDimension.preDetection();
        final String worldId = this.mapProcessor.getCurrentWorldId();
        if (worldId != null && !this.mapProcessor.isCurrentMapLocked()) {
            final String dimIdStr = this.mapProcessor.getDimensionName(dimId);
            final String mwId = this.mapProcessor.getCurrentMWId();
            final boolean multiplayer = this.mapProcessor.isWorldMultiplayer(this.mapProcessor.isWorldRealms(worldId), worldId);
            Path mapFolder = this.getMWSubFolder(worldId, dimIdStr, mwId);
            boolean mapFolderExists = mapFolder.toFile().exists();
            String multiplayerMapRegex = "^(-?\\d+)_(-?\\d+)\\.(zip|xaero)$";
            final MapLayer mainLayer = mapDimension.getLayeredMapRegions().getLayer(Integer.MAX_VALUE);
            if (multiplayer) {
                if (mapFolderExists) {
                    this.detectRegionsFromFiles(
                            mapDimension, worldId, dimIdStr, mwId, mapFolder, multiplayerMapRegex, 1, 2, 0, 20, new Consumer<RegionDetection>() {
                                public void accept(RegionDetection detect) {
                                    mainLayer.addRegionDetection(detect);
                                }
                            }
                    );
                }
            } else {
                File worldDir = this.mapProcessor.getWorldDataHandler().getWorldDir();
                if (worldDir == null) {
                    return;
                }

                Path worldFolder = worldDir.toPath().resolve("region");
                if (!worldFolder.toFile().exists()) {
                    return;
                }

                this.detectRegionsFromFiles(
                        mapDimension,
                        worldId,
                        dimIdStr,
                        mwId,
                        worldFolder,
                        "^r\\.(-{0,1}[0-9]+)\\.(-{0,1}[0-9]+)\\.mc[ar]$",
                        1,
                        2,
                        8192,
                        20,
                        mapDimension::addWorldSaveRegionDetection
                );
            }

            if (mapFolderExists) {
                Path cavesFolder = mapFolder.resolve("caves");

                try {
                    if (!Files.exists(cavesFolder)) {
                        Files.createDirectories(cavesFolder);
                    }

                    try (Stream<Path> cavesFolderStream = Files.list(cavesFolder)) {
                        cavesFolderStream.forEach(
                                new Consumer<Path>() {
                                    public void accept(Path layerFolder) {
                                        if (Files.isDirectory(layerFolder)) {
                                            String folderName = layerFolder.getFileName().toString();

                                            try {
                                                int layerInt = Integer.parseInt(folderName);
                                                final MapLayer layer = mapDimension.getLayeredMapRegions().getLayer(layerInt);
                                                if (multiplayer) {
                                                    detectRegionsFromFiles(
                                                            mapDimension,
                                                            worldId,
                                                            dimIdStr,
                                                            mwId,
                                                            layerFolder,
                                                            multiplayerMapRegex,
                                                            1,
                                                            2,
                                                            0,
                                                            20,
                                                            layer::addRegionDetection
                                                    );
                                                }
                                            } catch (NumberFormatException var5) {
                                            }
                                        }
                                    }
                                }
                        );
                    }
                } catch (IOException var27) {
                    WorldMap.LOGGER.error("IOException trying to detect map layers!");
                    if (attempts > 1) {
                        WorldMap.LOGGER.error("Retrying... " + --attempts);

                        try {
                            Thread.sleep(30L);
                        } catch (InterruptedException var23) {
                        }

                        this.detectRegionsInDimension(attempts, dimId);
                        return;
                    }

                    throw new RuntimeException("Couldn't detect map layers after multiple attempts.", var27);
                }
            }
        }
    }
}
