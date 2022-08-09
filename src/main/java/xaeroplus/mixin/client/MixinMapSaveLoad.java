package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
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
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mixin(value = MapSaveLoad.class, remap = false)
public abstract class MixinMapSaveLoad {
    private Instant start = Instant.now();

    @Shadow
    private MapProcessor mapProcessor;

    @Shadow
    private ArrayList<Path> cacheFolders;

    @Shadow
    public abstract Path getMWSubFolder(String world, String dim, String mw);

    @Shadow
    public abstract Path getCacheFolder(Path subFolder);

    @Inject(method = "detectRegions", at = @At("HEAD"))
    public void detectRegionsHead(CallbackInfo ci) {
        start = Instant.now();
    }

    @Inject(method = "detectRegions", at = @At("TAIL"))
    public void detectRegionsTail(CallbackInfo ci) {
        // this goes insanely slow when you have many regions
        // for me it takes 30+ seconds per dimension
        // even worse because it doesn't interrupt when dimension is changed
        // so on 2b2t if you quickly go through the queue, it'll finish loading the end regions before
        // starting on your actual ingame dimension regions
        WorldMap.LOGGER.info("Regions detected in " + (Instant.now().getEpochSecond() - start.getEpochSecond()) + " seconds");
    }

    /**
     * @author
     */
//    @Overwrite
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
        // this is a bit faster but hangs the whole process ><
        // ideally we'd want regions loaded lazily based on player position without blocking anything
        // todo: sync region loads around player position +- X regions, everything else load lazily
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
                // this is stupidly IO expensive
                try {
                    regionDetection.setCacheFile(this.getCacheFile(regionDetection, true, true));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.mapProcessor.addRegionDetection(mapDimension, regionDetection);
//                total.getAndIncrement();
            });
        } catch (final Exception e) {
            e.printStackTrace();
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

    /**
     * @author
     */
//    @Overwrite
    public File getCacheFile(MapRegionInfo region, boolean checkOldFolders, boolean requestCache) throws IOException {
        Path outdatedCacheFile;
        Path subFolder = this.getMWSubFolder(region.getWorldId(), region.getDimId(), region.getMwId());
        Path latestCacheFolder = this.getCacheFolder(subFolder);
        if (latestCacheFolder == null) {
            return null;
        }
        if (!Files.exists(latestCacheFolder)) {
            ForkJoinPool.commonPool().submit(() -> Files.createDirectories(latestCacheFolder));
        }
        Path cacheFile = latestCacheFolder.resolve(region.getRegionX() + "_" + region.getRegionZ() + ".xwmc");
        if (!checkOldFolders || Files.exists(cacheFile)) {
            return cacheFile.toFile();
        }
        if (requestCache) {
            region.setShouldCache(true, "cache file");
        }

        if (Files.exists(outdatedCacheFile = cacheFile.resolveSibling(cacheFile.getFileName().toString() + ".outdated"))) {
            return outdatedCacheFile.toFile();
        }
        // why is this even here
//        for (int i = 0; i < this.cacheFolders.size(); ++i) {
//            Path oldCacheFolder = this.cacheFolders.get(i);
//            Path oldCacheFile = oldCacheFolder.resolve(region.getRegionX() + "_" + region.getRegionZ() + ".xwmc");
//            if (!Files.exists(oldCacheFile)) continue;
//            return oldCacheFile.toFile();
//        }

        return cacheFile.toFile();
    }

}
