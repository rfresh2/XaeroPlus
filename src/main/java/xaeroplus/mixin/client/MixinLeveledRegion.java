package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static xaeroplus.util.Shared.decompressZipToBytes;

@Mixin(value = LeveledRegion.class, remap = false)
public abstract class MixinLeveledRegion<T extends RegionTexture<T>> {

    @Shadow
    protected abstract void writeCacheMetaData(DataOutputStream output, byte[] usableBuffer, byte[] integerByteBuffer);
    @Shadow
    public abstract T getTexture(int var1, int var2);
    @Shadow
    public abstract void setAllCachePrepared(boolean allCachePrepared);
    @Shadow
    protected File cacheFile;
    @Shadow
    protected boolean shouldCache;
    @Shadow
    protected abstract void readCacheMetaData(
            DataInputStream input, int cacheSaveVersion, byte[] usableBuffer, byte[] integerByteBuffer, boolean[][] textureLoaded, MapProcessor mapProcessor
    );
    @Shadow
    protected abstract void preCacheLoad();
    @Shadow
    protected abstract boolean shouldLeafAffectCache(int targetHighlightsHash);
    @Shadow
    protected abstract void readCacheInput(
            boolean isMeta,
            DataInputStream input,
            int cacheSaveVersion,
            byte[] usableBuffer,
            byte[] integerByteBuffer,
            boolean[][] textureLoaded,
            boolean leafShouldAffectBranches,
            MapProcessor mapProcessor
    );

    @Shadow
    public abstract void saveBiomePalette(DataOutputStream output) throws IOException;
    @Shadow
    protected abstract void loadBiomePalette(DataInputStream input, int cacheSaveVersion, MapProcessor mapProcessor) throws IOException;
    @Shadow
    protected abstract void onCacheLoadFailed(boolean[][] var1);

    /**
     * @author rfresh2
     * @reason efficient zip write
     */
    @Overwrite
    public boolean saveCacheTextures(File tempFile, int extraAttempts) throws IOException {
        if (WorldMap.settings.debug) {
            WorldMap.LOGGER.info("(World Map) Saving cache: " + this);
        }
        boolean success = false;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try(ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tempFile.toPath())))) {
            try(DataOutputStream output = new DataOutputStream(byteOut)) {
                ZipEntry e = new ZipEntry("cache.xaero");
                zipOutput.putNextEntry(e);
                byte[] usableBuffer = new byte[16384];
                byte[] integerByteBuffer = new byte[4];
                output.writeInt(24);
                this.writeCacheMetaData(output, usableBuffer, integerByteBuffer);
                this.saveBiomePalette(output);

                for (int i = 0; i < 8; ++i) {
                    for (int j = 0; j < 8; ++j) {
                        T texture = this.getTexture(i, j);
                        if (texture != null && texture.shouldIncludeInCache()) {
                            if (!texture.isCachePrepared()) {
                                throw new RuntimeException("Trying to save cache but " + i + " " + j + " in " + this + " is not prepared.");
                            }

                            output.write(i << 4 | j);
                            texture.writeCacheMapData(output, usableBuffer, integerByteBuffer, (LeveledRegion<T>) (Object) this);
                        }
                    }
                }

                output.write(255);
                zipOutput.write(byteOut.toByteArray());
                zipOutput.closeEntry();
                success = true;
            }
        } catch (IOException var51) {
            WorldMap.LOGGER.info("(World Map) IO exception while trying to save cache textures for " + this);
            if (extraAttempts > 0) {
                WorldMap.LOGGER.error("suppressed exception", var51);
                WorldMap.LOGGER.info("Retrying...");

                try {
                    Thread.sleep(20L);
                } catch (InterruptedException ignored) {
                }

                return this.saveCacheTextures(tempFile, extraAttempts - 1);
            } else {
                return false;
            }
        }
        synchronized (this) {
            this.setAllCachePrepared(false);
        }
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                T texture = this.getTexture(i, j);
                if (texture != null && texture.shouldIncludeInCache()) {
                    texture.deleteColorBuffer();
                    synchronized (this) {
                        texture.setCachePrepared(false);
                        this.setAllCachePrepared(false);
                    }
                }
            }
        }
        return success;
    }

    /**
     * @author rfresh2
     * @reason efficient zip reads
     */
    @Overwrite
    public boolean loadCacheTextures(
            MapProcessor mapProcessor,
            boolean justMetaData,
            boolean[][] textureLoaded,
            int targetHighlightsHash,
            boolean[] leafShouldAffectBranchesDest,
            boolean[] metaLoadedDest,
            int extraAttempts
    ) {
        if (this.cacheFile != null && this.cacheFile.exists()) {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(decompressZipToBytes(this.cacheFile.toPath())))) {
                byte[] integerByteBuffer = new byte[4];
                int cacheSaveVersion = input.readInt();
                if (cacheSaveVersion <= 24 && cacheSaveVersion != 7 && cacheSaveVersion != 21) {
                    if (cacheSaveVersion < 24) {
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
                    this.loadBiomePalette(input, cacheSaveVersion, mapProcessor);
                    boolean leafShouldAffectBranches = !this.shouldCache && this.shouldLeafAffectCache(targetHighlightsHash);
                    if (leafShouldAffectBranchesDest != null) {
                        leafShouldAffectBranchesDest[0] = leafShouldAffectBranches;
                    }

                    this.readCacheInput(
                            false, input, cacheSaveVersion, usableBuffer, integerByteBuffer, textureLoaded, leafShouldAffectBranches, mapProcessor
                    );
                    metaLoadedDest[0] = true;
                    return false;
                }

                WorldMap.LOGGER.info("(World Map) Trying to load newer region cache " + this + " using an older version of Xaero's World Map!");
                mapProcessor.getMapSaveLoad().backupFile(this.cacheFile, cacheSaveVersion);
                this.cacheFile = null;
                this.shouldCache = true;
                return true;
            } catch (IOException var60) {
                WorldMap.LOGGER.error("IO exception while trying to load cache for region " + this + "! " + this.cacheFile, var60);
                if (extraAttempts > 0) {
                    WorldMap.LOGGER.info("(World Map) Retrying...");

                    try {
                        Thread.sleep(20L);
                    } catch (InterruptedException ignored) {
                    }

                    metaLoadedDest[0] = false;
                    return this.loadCacheTextures(
                            mapProcessor, justMetaData, textureLoaded, targetHighlightsHash, leafShouldAffectBranchesDest, metaLoadedDest, extraAttempts - 1
                    );
                }
                this.onCacheLoadFailed(textureLoaded);
            } catch (Throwable var61) {
                WorldMap.LOGGER.error("Failed to load cache for region " + this + "! " + this.cacheFile, var61);
                this.onCacheLoadFailed(textureLoaded);
            }
        }
        this.cacheFile = null;
        this.shouldCache = true;
        return false;
    }
}
