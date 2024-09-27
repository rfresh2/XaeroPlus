package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.LongList;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.mcutils.version.MCVersion;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.BiomeFix;
import net.minecraft.util.datafix.fixes.CavesAndCliffsRenames;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.feature.render.highlights.SavableHighlightCacheInstance;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.world.level.Level.OVERWORLD;

public class OldBiomes extends Module {
    private final SavableHighlightCacheInstance oldBiomesCache = new SavableHighlightCacheInstance("XaeroPlusOldBiomes");
    // todo: configurable seed and MC version
    private static final long seed = -4172144997902289642L;
    private static final MCVersion mcVersion = MCVersion.v1_12_2;
    private final OverworldBiomeSource biomeSource = new OverworldBiomeSource(mcVersion, seed);
    private int oldBiomesColor = ColorHelper.getColor(0, 255, 0, 100);

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            this::getHighlightsSnapshot,
            this::getOldBiomesColor);
        oldBiomesCache.onEnable();
        try {
            searchAllLoadedChunks();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error searching all loaded chunks", e);
        }
    }

    @Override
    public void onDisable() {
        oldBiomesCache.onDisable();
        Globals.drawManager.unregisterChunkHighlightProvider(this.getClass());
    }

    @EventHandler
    public void onChunkDataEvent(final ChunkDataEvent event) {
        if (event.seenChunk()) return;
        try {
            if (event.chunk().getLevel().dimension() != OVERWORLD) return;
            searchBiomeAsync(event.chunk());
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking for OldBiome at chunk pos: [{}, {}]", event.chunk().getPos().x, event.chunk().getPos().z, e);
        }
    }

    private void searchBiomeAsync(ChunkAccess chunk) {
        Globals.moduleExecutorService.get().execute(() -> {
            try {
                searchBiome(chunk);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error checking for OldBiome at chunk pos: [{}, {}]", chunk.getPos().x, chunk.getPos().z, e);
            }
        });
    }

    // todo: benchmark performance to see if we should search async
    private void searchBiome(final ChunkAccess chunk) {
        var x = chunk.getPos().x;
        var z = chunk.getPos().z;
        if (oldBiomesCache.get().isHighlighted(x, z, ChunkUtils.getActualDimension())) return;
        var blockPosX = ChunkUtils.chunkCoordToCoord(x) + 1;
        var blockPosZ = ChunkUtils.chunkCoordToCoord(z) + 1;
        var blockPosY = 50;
        kaptainwutax.biomeutils.biome.Biome biome = biomeSource.getBiome(blockPosX, blockPosY, blockPosZ);
        if (biome == null) {
            XaeroPlus.LOGGER.error("Null biome returned from source at: {} {} {}", blockPosX, blockPosY, blockPosZ);
            return;
        }
        var oldBiomeName = biome.getName();
        // todo: skip fixup if we are using via to connect to a 1.12.2 server
        //  fixup only needs to be applied to biomes that are upgraded already by the server
        var oldBiomeFixup = fixupOldBiome(oldBiomeName);
        int sectionIndex = chunk.getSectionIndex(blockPosY);
        var containsBiome = new AtomicBoolean(false);
        var palettedContainerR0 = chunk.getSection(sectionIndex).getBiomes();
        // ideally we would only need to check the palette instead of the full data entries set
        // but due to the same mechanics as Palette NewChunks there can be a nonexistent plains biome entry present
        if (palettedContainerR0 instanceof PalettedContainer<Holder<Biome>> palettedContainer) {
            // could and probably should be replaced by a for loop handling each bitstorage type
            // would remove the lambda allocation and we could exit the loop immediately on finding a match
            palettedContainer.data.storage().getAll(i -> {
                if (containsBiome.get()) return;
                var holder = palettedContainer.data.palette().valueFor(i);
                var match = holder.unwrapKey()
                    .map(ResourceKey::location)
                    .map(ResourceLocation::getPath)
                    .filter(path -> path.equals(oldBiomeFixup))
                    .isPresent();
                containsBiome.set(match);
            });
        }
        if (containsBiome.get()) oldBiomesCache.get().addHighlight(x, z);
    }

    private String fixupOldBiome(String oldBiome) {
        var oldBiomeFixupNamespaced = "minecraft:" + oldBiome;
        oldBiomeFixupNamespaced = BiomeFix.BIOMES.getOrDefault(oldBiomeFixupNamespaced, oldBiomeFixupNamespaced);
        oldBiomeFixupNamespaced = CavesAndCliffsRenames.RENAMES.getOrDefault(oldBiomeFixupNamespaced, oldBiomeFixupNamespaced);
        return oldBiomeFixupNamespaced.split("minecraft:")[1];
    }

    private void searchAllLoadedChunks() {
        if (mc.level == null || ChunkUtils.getActualDimension() != OVERWORLD) return;
        final int renderDist = mc.options.renderDistance().get();
        final int xMin = ChunkUtils.getPlayerChunkX() - renderDist;
        final int xMax = ChunkUtils.getPlayerChunkX() + renderDist;
        final int zMin = ChunkUtils.getPlayerChunkZ() - renderDist;
        final int zMax = ChunkUtils.getPlayerChunkZ() + renderDist;
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                ChunkAccess chunk = mc.level.getChunkSource().getChunk(x, z, false);
                if (chunk instanceof EmptyLevelChunk) continue;
                if (chunk == null) continue;
                searchBiomeAsync(chunk);
            }
        }
    }

    public int getOldBiomesColor() {
        return oldBiomesColor;
    }

    public LongList getHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return oldBiomesCache.get().getHighlightsSnapshot(dimension);
    }

    public void setDiskCache(final boolean b) {
        oldBiomesCache.setDiskCache(b, isEnabled());
    }

    public void setAlpha(final double b) {
        oldBiomesColor = ColorHelper.getColorWithAlpha(oldBiomesColor, (int) b);
    }

    public void setRgbColor(final int color) {
        oldBiomesColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.oldBiomesAlphaSetting.getAsInt());
    }
}
