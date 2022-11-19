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
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

    /**
     * @author rfresh2
     * @reason efficient zip write
     */
    @Overwrite
    public boolean saveCacheTextures(File tempFile, int extraAttempts) throws IOException {
        if (WorldMap.settings.debug) {
            WorldMap.LOGGER.info("(World Map) Saving cache: " + this);
        }

        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ZipOutputStream zipOutput = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
            Throwable var4 = null;

            boolean success = false;
            try {
                DataOutputStream output = new DataOutputStream(byteOut);
                Throwable var6 = null;
                try {
                    ZipEntry e = new ZipEntry("cache.xaero");
                    zipOutput.putNextEntry(e);
                    byte[] usableBuffer = new byte[16384];
                    byte[] integerByteBuffer = new byte[4];
                    output.writeInt(18);
                    this.writeCacheMetaData(output, usableBuffer, integerByteBuffer);

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

                    success = true;
                } catch (Throwable var47) {
                    var6 = var47;
                    throw var47;
                } finally {
                    if (output != null) {
                        if (var6 != null) {
                            try {
                                output.close();
                            } catch (Throwable var44) {
                                var6.addSuppressed(var44);
                            }
                        } else {
                            output.close();
                        }
                    }

                }
            } catch (Throwable var49) {
                var4 = var49;
                throw var49;
            } finally {
                if (zipOutput != null) {
                    if (var4 != null) {
                        try {
                            zipOutput.close();
                        } catch (Throwable var43) {
                            var4.addSuppressed(var43);
                        }
                    } else {
                        zipOutput.close();
                    }
                }
                byteOut.close();


            }

            return success;
        } catch (IOException var51) {
            WorldMap.LOGGER.info("(World Map) IO exception while trying to save cache textures for " + this);
            if (extraAttempts > 0) {
                WorldMap.LOGGER.error("suppressed exception", var51);
                WorldMap.LOGGER.info("Retrying...");

                try {
                    Thread.sleep(20L);
                } catch (InterruptedException var42) {
                }

                return this.saveCacheTextures(tempFile, extraAttempts - 1);
            } else {
                throw var51;
            }
        }
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
        if (this.cacheFile == null) {
            return false;
        } else {
            if (this.cacheFile.exists()) {
                try {

                    Throwable var9 = null;
                    byte[] usableBuffer;
                    DataInputStream input = null;
                    try {
                        input = new DataInputStream(new ByteArrayInputStream(decompressZipToBytes(this.cacheFile.toPath())));
                        Throwable var11 = null;

                        try {
                            byte[] integerByteBuffer = new byte[4];
                            int cacheSaveVersion = input.readInt();
                            if (cacheSaveVersion <= 18 && cacheSaveVersion != 7) {
                                if (cacheSaveVersion < 18) {
                                    this.shouldCache = true;
                                }

                                usableBuffer = new byte[16384];
                                if (cacheSaveVersion >= 8) {
                                    this.readCacheMetaData(input, cacheSaveVersion, usableBuffer, integerByteBuffer, textureLoaded, mapProcessor);
                                    metaLoadedDest[0] = true;
                                    if (justMetaData && (cacheSaveVersion == 8 || cacheSaveVersion >= 12)) {
                                        boolean leafShouldAffectBranches = true;
                                        return leafShouldAffectBranches;
                                    }
                                }

                                this.preCacheLoad();
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

                            input.close();
                            WorldMap.LOGGER.info("(World Map) Trying to load newer region cache " + this + " using an older version of Xaero's World Map!");
                            mapProcessor.getMapSaveLoad().backupFile(this.cacheFile, cacheSaveVersion);
                            this.cacheFile = null;
                            this.shouldCache = true;
                        } catch (Throwable var56) {
                            var11 = var56;
                            throw var56;
                        } finally {
                            if (input != null) {
                                if (var11 != null) {
                                    try {
                                        input.close();
                                    } catch (Throwable var55) {
                                        var11.addSuppressed(var55);
                                    }
                                } else {
                                    input.close();
                                }
                            }

                        }
                    } catch (Throwable var58) {
                        var9 = var58;
                        throw var58;
                    } finally {
                        if (input != null) {
                            input.close();
                        }
                    }

                    return true;
                } catch (IOException var60) {
                    WorldMap.LOGGER.error("IO exception while trying to load cache for region " + this + "! " + this.cacheFile, var60);
                    if (extraAttempts > 0) {
                        WorldMap.LOGGER.info("(World Map) Retrying...");

                        try {
                            Thread.sleep(20L);
                        } catch (InterruptedException var53) {
                        }

                        metaLoadedDest[0] = false;
                        return this.loadCacheTextures(
                                mapProcessor, justMetaData, textureLoaded, targetHighlightsHash, leafShouldAffectBranchesDest, metaLoadedDest, extraAttempts - 1
                        );
                    }

                    this.cacheFile = null;
                    this.shouldCache = true;
                } catch (Throwable var61) {
                    this.cacheFile = null;
                    this.shouldCache = true;
                    WorldMap.LOGGER.error("Failed to load cache for region " + this + "! " + this.cacheFile, var61);
                }
            } else {
                this.cacheFile = null;
                this.shouldCache = true;
            }

            return false;
        }
    }

    private static byte[] decompressZipToBytes(final Path input) {
        try {
            return toUnzippedByteArray(Files.readAllBytes(input));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] toUnzippedByteArray(byte[] zippedBytes) throws IOException {
        final ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zippedBytes));
        final byte[] buff = new byte[1024];
        if (zipInputStream.getNextEntry() != null) {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int l;
            while ((l = zipInputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, l);
            }
            return outputStream.toByteArray();
        }
        return new byte[0];
    }
}
