package xaeroplus.mixin.client;

import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Mixin(value = LeveledRegion.class, remap = false)
public abstract class MixinLeveledRegion<T extends RegionTexture<T>> implements Comparable<LeveledRegion<T>> {

    @Shadow
    protected abstract void writeCacheMetaData(DataOutputStream output, byte[] usableBuffer, byte[] integerByteBuffer) throws IOException;

    @Shadow
    public abstract void setAllCachePrepared(boolean allCachePrepared);

    @Shadow
    public abstract T getTexture(int var1, int var2);

    @Shadow
    protected File cacheFile;

    @Shadow
    protected boolean shouldCache;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean saveCacheTextures(File tempFile, int extraAttempts) throws IOException {
        if (WorldMap.settings.debug) {
            WorldMap.LOGGER.info("(World Map) Saving cache: " + this);
        }

        try (DataOutputStream output = new DataOutputStream(new FramedLZ4CompressorOutputStream(new FileOutputStream(tempFile)))) {
            byte[] usableBuffer = new byte[16384];
            byte[] integerByteBuffer = new byte[4];
            output.writeInt(18);
            this.writeCacheMetaData(output, usableBuffer, integerByteBuffer);

            int i;
            int j;
            RegionTexture texture;
            for (i = 0; i < 8; ++i) {
                for (j = 0; j < 8; ++j) {
                    texture = this.getTexture(i, j);
                    if (texture != null && texture.shouldIncludeInCache()) {
                        if (!texture.isCachePrepared()) {
                            throw new RuntimeException("Trying to save cache but " + i + " " + j + " in " + this + " is not prepared.");
                        }

                        output.write(i << 4 | j);
                        texture.writeCacheMapData(output, usableBuffer, integerByteBuffer, (LeveledRegion) (Object) this);
                    }
                }
            }

            output.write(255);
            synchronized (this) {
                this.setAllCachePrepared(false);
            }

            for (i = 0; i < 8; ++i) {
                for (j = 0; j < 8; ++j) {
                    texture = this.getTexture(i, j);
                    if (texture != null && texture.shouldIncludeInCache()) {
                        texture.deleteColorBuffer();
                        synchronized (this) {
                            texture.setCachePrepared(false);
                            this.setAllCachePrepared(false);
                        }
                    }
                }
            }

            return true;

        } catch (IOException e) {
            WorldMap.LOGGER.info("(World Map) IO exception while trying to save cache textures for " + this);
            if (extraAttempts > 0) {
                WorldMap.LOGGER.error("suppressed exception", e);
                WorldMap.LOGGER.info("Retrying...");

                try {
                    Thread.sleep(20L);
                } catch (InterruptedException var42) {
                }

                return this.saveCacheTextures(tempFile, extraAttempts - 1);
            } else {
                throw e;
            }
        }
    }

    @Shadow
    protected abstract void readCacheMetaData(DataInputStream input, int cacheSaveVersion, byte[] usableBuffer, byte[] integerByteBuffer, boolean[][] textureLoaded, MapProcessor mapProcessor) throws IOException;

    @Shadow
    protected abstract void preCacheLoad();

    @Shadow
    protected abstract boolean shouldLeafAffectCache(int targetHighlightsHash);

    @Shadow
    protected abstract void readCacheInput(boolean isMeta, DataInputStream input, int cacheSaveVersion, byte[] usableBuffer, byte[] integerByteBuffer, boolean[][] textureLoaded, boolean leafShouldAffectBranches, MapProcessor mapProcessor) throws IOException;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean loadCacheTextures(MapProcessor mapProcessor, boolean justMetaData, boolean[][] textureLoaded, int targetHighlightsHash, boolean[] leafShouldAffectBranchesDest, boolean[] metaLoadedDest, int extraAttempts) {
        if (this.cacheFile == null) {
            return false;
        }
        if (!this.cacheFile.exists()) {
            this.cacheFile = null;
            this.shouldCache = true;
            return false;
        }
        try (DataInputStream input = new DataInputStream(new FramedLZ4CompressorInputStream(new FileInputStream(this.cacheFile)))) {
            boolean leafShouldAffectBranches;
            byte[] integerByteBuffer = new byte[4];
            int cacheSaveVersion = input.readInt();
            if (cacheSaveVersion > 18 || cacheSaveVersion == 7) {
                input.close();
                WorldMap.LOGGER.info("(World Map) Trying to load newer region cache " + this + " using an older version of Xaero's World Map!");
                mapProcessor.getMapSaveLoad().backupFile(this.cacheFile, cacheSaveVersion);
                this.cacheFile = null;
                this.shouldCache = true;
                return false;
            }
            if (cacheSaveVersion < 18) {
                this.shouldCache = true;
            }
            byte[] usableBuffer = new byte[16384];
            if (cacheSaveVersion >= 8) {
                this.readCacheMetaData(input, cacheSaveVersion, usableBuffer, integerByteBuffer, textureLoaded, mapProcessor);
                metaLoadedDest[0] = true;
                if (justMetaData && (cacheSaveVersion == 8 || cacheSaveVersion >= 12)) {
                    return true;
                }
            }
            this.preCacheLoad();
            leafShouldAffectBranches = !this.shouldCache && this.shouldLeafAffectCache(targetHighlightsHash);
            if (leafShouldAffectBranchesDest != null) {
                leafShouldAffectBranchesDest[0] = leafShouldAffectBranches;
            }
            this.readCacheInput(false, input, cacheSaveVersion, usableBuffer, integerByteBuffer, textureLoaded, leafShouldAffectBranches, mapProcessor);
            metaLoadedDest[0] = true;
            return false;
        } catch (final IOException e) {
            WorldMap.LOGGER.error("IO exception while trying to load cache for region " + this + "! " + this.cacheFile, e);
            if (extraAttempts <= 0) {
                this.cacheFile = null;
                this.shouldCache = true;
                return false;
            }
            WorldMap.LOGGER.info("(World Map) Retrying...");
            try {
                Thread.sleep(20L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
            metaLoadedDest[0] = false;
            return this.loadCacheTextures(mapProcessor, justMetaData, textureLoaded, targetHighlightsHash, leafShouldAffectBranchesDest, metaLoadedDest, extraAttempts - 1);

        } catch (Throwable e) {
            this.cacheFile = null;
            this.shouldCache = true;
            WorldMap.LOGGER.error("Failed to load cache for region " + this + "! " + this.cacheFile, e);
            return false;
        }
    }
}
