package xaeroplus.mixin.client;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.file.OldFormatSupport;
import xaero.map.palette.FastPalette;
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
    private FastPalette<RegistryKey<Biome>> biomePalette;
    @Shadow
    protected abstract void readCacheMetaData(
            DataInputStream input,
            int minorSaveVersion,
            int majorSaveVersion,
            byte[] usableBuffer,
            byte[] integerByteBuffer,
            boolean[][] textureLoaded,
            MapProcessor mapProcessor
    );
    @Shadow
    protected abstract void preCacheLoad();
    @Shadow
    protected abstract boolean shouldLeafAffectCache(int targetHighlightsHash);
    @Shadow
    protected abstract void readCacheInput(
            boolean isMeta,
            DataInputStream input,
            int minorSaveVersion,
            int majorSaveVersion,
            byte[] usableBuffer,
            byte[] integerByteBuffer,
            boolean[][] textureLoaded,
            boolean leafShouldAffectBranches,
            MapProcessor mapProcessor
    );

    @Shadow
    public abstract void saveBiomePalette(DataOutputStream output) throws IOException;
    @Shadow
    protected abstract void loadBiomePalette(DataInputStream input,
                                             int minorSaveVersion,
                                             int majorSaveVersion,
                                             MapProcessor mapProcessor,
                                             Registry<Biome> biomeRegistry,
                                             OldFormatSupport oldFormatSupport) throws IOException;
    @Shadow
    protected abstract void onCacheLoadFailed(boolean[][] var1);

    @Shadow public abstract boolean hasTextures();

    /**
     * @author rfresh2
     * @reason efficient zip write
     */
    @Overwrite
    public boolean saveCacheTextures(File tempFile, int extraAttempts) throws IOException {
        if (WorldMap.settings.debug) {
            WorldMap.LOGGER.info("(World Map) Saving cache: " + this);
        }
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try(ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tempFile.toPath())))) {
            try(DataOutputStream output = new DataOutputStream(byteOut)) {
                ZipEntry e = new ZipEntry("cache.xaero");
                zipOutput.putNextEntry(e);
                byte[] usableBuffer = new byte[16384];
                byte[] integerByteBuffer = new byte[4];
                int currentFullVersion = 65560;
                output.writeInt(currentFullVersion);
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
                return true;
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
    }

    /**
     * @author rfresh2
     * @reason efficient zip reads
     */
//    @Overwrite
    public boolean loadCacheTexturesOld(
            MapProcessor mapProcessor,
            Registry<Biome> biomeRegistry,
            boolean justMetaData,
            boolean[][] textureLoaded,
            int targetHighlightsHash,
            boolean[] leafShouldAffectBranchesDest,
            boolean[] metaLoadedDest,
            int extraAttempts,
            OldFormatSupport oldFormatSupport
    ) {
        if (this.cacheFile != null && this.cacheFile.exists()) {
            try {
                try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(decompressZipToBytes(this.cacheFile.toPath())))) {
                    byte[] integerByteBuffer = new byte[4];
                    int cacheSaveVersion = input.readInt();
                    int cacheFullSaveVersion = input.readInt();
                    int minorSaveVersion = cacheFullSaveVersion & 65535;
                    int majorSaveVersion = cacheFullSaveVersion >> 16 & 65535;
                    int currentFullVersion = 65560;

                    if (cacheFullSaveVersion <= currentFullVersion && cacheFullSaveVersion != 7 && minorSaveVersion != 21) {
                        if (cacheFullSaveVersion < currentFullVersion) {
                            this.shouldCache = true;
                        }
                        this.biomePalette = null;
                        byte[] usableBuffer = new byte[16384];
                        if (minorSaveVersion >= 8) {
                            this.readCacheMetaData(input, minorSaveVersion, majorSaveVersion, usableBuffer, integerByteBuffer, textureLoaded, mapProcessor);
                            metaLoadedDest[0] = true;
                            if (justMetaData && (cacheSaveVersion == 8 || cacheSaveVersion >= 12)) {
                                return true;
                            }
                        }

                        this.preCacheLoad();
                        this.loadBiomePalette(input, minorSaveVersion, majorSaveVersion, mapProcessor, biomeRegistry, oldFormatSupport);
                        boolean leafShouldAffectBranches = !this.shouldCache && this.shouldLeafAffectCache(targetHighlightsHash);
                        if (leafShouldAffectBranchesDest != null) {
                            leafShouldAffectBranchesDest[0] = leafShouldAffectBranches;
                        }

                        this.readCacheInput(
                                false,
                                input,
                                minorSaveVersion,
                                majorSaveVersion,
                                usableBuffer,
                                integerByteBuffer,
                                textureLoaded,
                                leafShouldAffectBranches,
                                mapProcessor
                        );
                        metaLoadedDest[0] = true;
                        return false;
                    }

                    WorldMap.LOGGER.info("(World Map) Trying to load newer region cache " + this + " using an older version of Xaero's World Map!");
                    mapProcessor.getMapSaveLoad().backupFile(this.cacheFile, cacheSaveVersion);
                    this.cacheFile = null;
                    this.shouldCache = true;
                    return true;
                } finally {
                    if (hasTextures()) {
                        for(int i = 0; i < 8; ++i) {
                            for (int j = 0; j < 8; ++j) {
                                RegionTexture<?> texture = getTexture(i, j);
                                if (texture != null && texture.getBiomes() != null && texture.getBiomes().getRegionBiomePalette() != this.biomePalette) {
                                    texture.resetBiomes();
                                }
                            }
                        }
                    }
                }
            } catch (IOException var60) {
                WorldMap.LOGGER.error("IO exception while trying to load cache for region " + this + "! " + this.cacheFile, var60);
                if (extraAttempts > 0) {
                    WorldMap.LOGGER.info("(World Map) Retrying...");

                    try {
                        Thread.sleep(20L);
                    } catch (InterruptedException ignored) {
                    }

                    metaLoadedDest[0] = false;
                    // todo
//                    return this.loadCacheTextures(
//                            mapProcessor,
//                            biomeRegistry,
//                            justMetaData,
//                            textureLoaded,
//                            targetHighlightsHash,
//                            leafShouldAffectBranchesDest,
//                            metaLoadedDest,
//                            extraAttempts - 1,
//                            oldFormatSupport
//                    );
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
