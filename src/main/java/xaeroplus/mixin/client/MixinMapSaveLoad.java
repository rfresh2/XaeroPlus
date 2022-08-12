package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.file.MapRegionInfo;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.RegionDetection;
import xaero.map.world.MapDimension;

import java.io.File;
import java.io.IOException;
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
     */
    @Overwrite
    public void updateCacheFolderList(Path subFolder) {
        // kek
        // overwriting this doesn't actually disable caching
        // resolves massive hangs due to terrible original code in this method
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
