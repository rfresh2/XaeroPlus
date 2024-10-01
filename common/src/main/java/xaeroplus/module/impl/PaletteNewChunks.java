package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongList;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.feature.render.highlights.SavableHighlightCacheInstance;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import static net.minecraft.world.level.Level.*;
import static xaeroplus.module.impl.PaletteNewChunks.BiomeCheckResult.*;
import static xaeroplus.util.ColorHelper.getColor;

public class PaletteNewChunks extends Module {
    private final SavableHighlightCacheInstance newChunksCache = new SavableHighlightCacheInstance("XaeroPlusPaletteNewChunks");
    private final SavableHighlightCacheInstance newChunksInverseCache = new SavableHighlightCacheInstance("XaeroPlusPaletteNewChunksInverse");
    private int newChunksColor = getColor(255, 0, 0, 100);
    private final IntSet presentStateIdsBuf = new IntOpenHashSet();
    private boolean renderInverse = false;

    public void setDiskCache(final boolean disk) {
        newChunksCache.setDiskCache(disk, isEnabled());
        newChunksInverseCache.setDiskCache(disk, isEnabled());
    }

    @EventHandler
    public void onChunkData(ChunkDataEvent event) {
        if (event.seenChunk()) return; // never will be newchunk if we've already cached it
        var dim = ChunkUtils.getActualDimension();
        var chunk = event.chunk();
        var x = chunk.getPos().x;
        var z = chunk.getPos().z;
        try {
            if (newChunksCache.get().isHighlighted(x, z, dim)) return;
            if (newChunksInverseCache.get().isHighlighted(x, z, dim)) return;
            if (isNewChunk(dim, chunk)) newChunksCache.get().addHighlight(x, z);
            else newChunksInverseCache.get().addHighlight(x, z);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking palette NewChunk at [{} {}]", x, z, e);
        }
    }

    private boolean isNewChunk(final ResourceKey<Level> dim, final LevelChunk chunk) {
        if (dim == OVERWORLD) {
            return switch (checkNewChunkBiomePalette(chunk, true)) {
                case NO_PLAINS -> false;
                case PLAINS_IN_PALETTE -> true;
                case PLAINS_PRESENT -> checkNewChunkBlockStatePalette(chunk);
            };
        } else if (dim == NETHER) {
            return checkNewChunkBiomePalette(chunk, false) == PLAINS_IN_PALETTE;
        } else if (dim == END) {
            return checkNewChunkBiomePalette(chunk, false) == PLAINS_IN_PALETTE;
        }
        return false;
    }

    /**
     * MC generates chunks in multiple steps where each step progressively mutates the chunk data
     * For more info see this explanation by Henrik Kniberg: https://youtu.be/ob3VwY4JyzE&t=453
     *
     * When a chunk is first generated it is populated first by air, then by additional block types like stone, water, etc
     * By the end of these steps, the chunk's blockstate palette will still contain references to all states that were ever present
     * For more info on what chunk palettes are see: https://wiki.vg/Chunk_Format#Paletted_Container_structure
     *
     * When the MC server writes + reads the chunks to region files it compacts the palette to save disk space
     * the key is that this compaction occurs _after_ newly generated chunk data is sent to players
     *
     * compacting has 2 effects:
     * 1. palette entries without values present in the chunk are removed
     * 2. the order of ids in the palette can change as it is rebuilt in order of the actual blockstates present in the chunk
     *
     * So we are simply checking if the first entry of the lowest section's block palette is air
     * The lowest section should always have bedrock as the first entry at the bottom section after compacting
     * Credits to etianl (https://github.com/etianl/Trouser-Streak) for first idea and public implementation for examining palette entries
     * and crosby (https://github.com/RacoonDog) for idea to check if air is the first palette entry
     *
     * However, there is a chance for false negatives if the chunk's palette generates with more than 16 different blockstates
     * The palette gets resized to a HashMapPalette which does not retain the original entry ordering
     * Usually this happens when features like mineshafts or the deep dark generates
     *
     * The second check that can be applied is verifying every palette entry is actually present in the data.
     * But this can still fail if air is still present in the section. Or if the chunk is modified by a
     * different online player right before we enter it.
     */
    private boolean checkNewChunkBlockStatePalette(LevelChunk chunk) {
        var sections = chunk.getSections();
        if (sections.length == 0) return false;
        var firstSection = sections[0];
        Palette<BlockState> firstPalette = firstSection.getStates().data.palette();
        if (isNotLinearOrHashMapPalette(firstPalette)) return false;
        if (firstPalette instanceof LinearPalette<BlockState>) {
            return firstPalette.valueFor(0).is(Blocks.AIR);
        } else { // HashMapPalette
            // we could iterate through more sections but this is good enough in most cases
            // checking every blockstate is relatively expensive
            for (int i = 0; i < Math.min(sections.length, 5); i++) {
                var section = sections[i];
                var paletteContainerData = section.getStates().data;
                var palette = paletteContainerData.palette();
                if (isNotLinearOrHashMapPalette(palette)) continue;
                if (checkForExtraPaletteEntries(paletteContainerData)) return true;
            }
        }
        return false;
    }

    /**
     * Same logic as BlockState palette but we check the biomes palette.
     * MC initializes palettes with the Plains biome.
     *
     * This check is very reliable in all dimensions - even the overworld as long as plains is not a real biome present.
     *
     * This should generally be preferred over blockstate palette checks as its faster and more reliable.
     * For example, this solves the issue of player activity modifying the chunk, and therefore possibly causing palette ID's
     * without matching data present, at the same time as we load them.
     */
    private BiomeCheckResult checkNewChunkBiomePalette(LevelChunk chunk, boolean checkData) {
        var sections = chunk.getSections();
        if (sections.length == 0) return NO_PLAINS;
        var firstSection = sections[0];
        var biomes = firstSection.getBiomes();
        if (biomes instanceof PalettedContainer<Holder<Biome>> biomesPaletteContainer) {
            var palette = biomesPaletteContainer.data.palette();
            boolean paletteContainsPlains = palette.maybeHas(PaletteNewChunks::isPlainsBiome);
            if (paletteContainsPlains && checkData) {
                if (palette.getSize() == 1) return PLAINS_PRESENT;
                var storage = biomesPaletteContainer.data.storage();
                presentStateIdsBuf.clear();
                storage.getAll(presentStateIdsBuf::add);
                for (int id : presentStateIdsBuf) {
                    if (isPlainsBiome(palette.valueFor(id))) {
                        return PLAINS_PRESENT;
                    }
                }
            }
            if (paletteContainsPlains) return PLAINS_IN_PALETTE;
        }
        return NO_PLAINS;
    }

    enum BiomeCheckResult {
        NO_PLAINS,
        PLAINS_IN_PALETTE,
        PLAINS_PRESENT
    }

    private boolean isNotLinearOrHashMapPalette(Palette palette) {
        return palette.getSize() <= 0 || !(palette instanceof LinearPalette || palette instanceof HashMapPalette);
    }

    private synchronized boolean checkForExtraPaletteEntries(PalettedContainer.Data<BlockState> paletteContainer) {
        presentStateIdsBuf.clear(); // reusing to reduce gc pressure
        var palette = paletteContainer.palette();
        BitStorage storage = paletteContainer.storage();
        storage.getAll(presentStateIdsBuf::add);
        return palette.getSize() > presentStateIdsBuf.size();
    }

    private static boolean isPlainsBiome(Holder<Biome> holder) {
        return holder.is(Biomes.PLAINS);
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            this::getHighlightsSnapshot,
            this::getNewChunksColor);
        newChunksCache.onEnable();
        newChunksInverseCache.onEnable();
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
        newChunksInverseCache.onDisable();
        Globals.drawManager.unregisterChunkHighlightProvider(this.getClass());
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.paletteNewChunksAlphaSetting.getAsInt());
    }

    public void setAlpha(final double a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
    }

    public void setInverse(final boolean b) {
        renderInverse = b;
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return renderInverse
            ? isInverseNewChunk(chunkPosX, chunkPosZ, dimensionId)
            : isNewChunk(chunkPosX, chunkPosZ, dimensionId);
    }

    public LongList getHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return renderInverse
            ? newChunksInverseCache.get().getHighlightsSnapshot(dimension)
            : newChunksCache.get().getHighlightsSnapshot(dimension);
    }

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksCache.get().isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isInverseNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksInverseCache.get().isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
