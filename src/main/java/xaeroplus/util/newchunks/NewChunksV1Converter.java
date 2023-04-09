package xaeroplus.util.newchunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import xaero.map.WorldMap;
import xaeroplus.XaeroPlus;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static xaeroplus.util.ChunkUtils.longToChunkX;
import static xaeroplus.util.ChunkUtils.longToChunkZ;

public class NewChunksV1Converter {
    private static final String NEWCHUNKS_FILE_NAME = "XaeroPlusNewChunksV1.data";

    public static void convert(final NewChunksSavingCache cache, final String worldId, final String mwId) {
        ForkJoinPool.commonPool().execute(() -> {
            Path ow = getV1Path("DIM0", worldId, mwId);
            convertDimension(ow, 0, cache);
            Path ow2 = getV1Path("null", worldId, mwId);
            convertDimension(ow2, 0, cache);
            Path nether = getV1Path("DIM-1", worldId, mwId);
            convertDimension(nether, -1, cache);
            Path end = getV1Path("DIM1", worldId, mwId);
            convertDimension(end, 1, cache);
        });
    }

    private static Path getV1Path(final String dimensionId, final String worldId, final String mwId) {
        Path mainXaeroWorldMapFolder = WorldMap.saveFolder.toPath();
        try {
            return mainXaeroWorldMapFolder.resolve(worldId).resolve(dimensionId).resolve(mwId).resolve(NEWCHUNKS_FILE_NAME);
        } catch (final Exception e) {
            return null;
        }
    }

    private static void convertDimension(final Path saveFile, final int dimensionId, final NewChunksSavingCache cache) {
        if (saveFile == null || !Files.exists(saveFile)) {
            return;
        }
        final Gson gson = new GsonBuilder().create();
        final TypeToken<List<NewChunkDataV1>> newChunkDataType = new TypeToken<List<NewChunkDataV1>>() { };
        try (Reader reader = new InputStreamReader(new FramedLZ4CompressorInputStream(Files.newInputStream(saveFile)))) {
            final List<NewChunkDataV1> newChunkData = gson.fromJson(reader, newChunkDataType.getType());
            newChunkData.forEach(n -> {
                try {
                    cache.addNewChunk(longToChunkX(n.chunkPos), longToChunkZ(n.chunkPos), n.foundTime, dimensionId);
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Error converting new chunks file for dimension " + dimensionId, e);
                }
            });
            XaeroPlus.LOGGER.info("Converted new chunks file for dimension " + dimensionId);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error converting new chunks file for dimension " + dimensionId, e);
        }
        try {
            moveFileToBackup(saveFile);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error moving old new chunks file to backup", e);
        }
    }

    private static void moveFileToBackup(final Path saveFile) {
        try {
            Files.move(saveFile, saveFile.resolveSibling(saveFile.getFileName() + ".bak"));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error moving old new chunks file to backup", e);
        }
    }

    private static class NewChunkDataV1 {
        public final Long chunkPos;
        public final Long foundTime;

        public NewChunkDataV1(final Long chunkPos, final Long foundTime) {
            this.chunkPos = chunkPos;
            this.foundTime = foundTime;
        }
    }
}
