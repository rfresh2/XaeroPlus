package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
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
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.render.ChunkHighlightProvider;
import xaeroplus.feature.render.ColorHelper;
import xaeroplus.feature.render.highlights.ChunkHighlightCache;
import xaeroplus.feature.render.highlights.ChunkHighlightLocalCache;
import xaeroplus.feature.render.highlights.ChunkHighlightSavingCache;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;

import static net.minecraft.world.level.Level.*;
import static xaeroplus.feature.render.ColorHelper.getColor;

public class PaletteNewChunks extends Module {
    private ChunkHighlightCache newChunksCache = new ChunkHighlightLocalCache();
    private ChunkHighlightCache newChunksInverseCache = new ChunkHighlightLocalCache();
    private int newChunksColor = getColor(255, 0, 0, 100);
    private static final String DATABASE_NAME = "XaeroPlusPaletteNewChunks";
    private static final String INVERSE_DATABASE_NAME = "XaeroPlusPaletteNewChunksInverse";
    private final IntSet presentStateIdsBuf = new IntOpenHashSet();
    private boolean renderInverse = false;

    public void setNewChunksCache(final boolean disk) {
        try {
            final Long2LongMap map = newChunksCache.getHighlightsState();
            newChunksCache.onDisable();
            if (disk) {
                newChunksCache = new ChunkHighlightSavingCache(DATABASE_NAME);
            } else {
                newChunksCache = new ChunkHighlightLocalCache();
            }
            if (this.isEnabled()) {
                newChunksCache.onEnable();
                if (map != null) newChunksCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing palette new chunks cache", e);
        }
        try {
            final Long2LongMap map = newChunksInverseCache.getHighlightsState();
            newChunksInverseCache.onDisable();
            if (disk) {
                newChunksInverseCache = new ChunkHighlightSavingCache(INVERSE_DATABASE_NAME);
            } else {
                newChunksInverseCache = new ChunkHighlightLocalCache();
            }
            if (this.isEnabled()) {
                newChunksInverseCache.onEnable();
                if (map != null) newChunksInverseCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing palette inverser new chunks cache", e);
        }
    }

    @EventHandler
    public void onChunkData(ChunkDataEvent event) {
        if (event.seenChunk()) return; // never will be newchunk if we've already cached it
        var currentDim = ChunkUtils.getActualDimension();
        var x = event.chunk().getPos().x;
        var z = event.chunk().getPos().z;
        try {
            if (newChunksCache.isHighlighted(x, z, currentDim)) return;
            if (newChunksInverseCache.isHighlighted(x, z, currentDim)) return;
            if (currentDim == OVERWORLD || currentDim == NETHER) {
                if (checkNewChunkOverworldOrNether(event.chunk())) {
                    newChunksCache.addHighlight(x, z);
                } else {
                    newChunksInverseCache.addHighlight(x, z);
                }
            } else if (currentDim == END) {
                if (checkNewChunkEnd(event.chunk())) {
                    newChunksCache.addHighlight(x, z);
                } else {
                    newChunksInverseCache.addHighlight(x, z);
                }
            }
        } catch (final Exception e) {
            // fall through
        }
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
     * 1. palette entries without blockstates present in the chunk are removed
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
     * To catch these we fall back to checking if the palette has more entries than what is actually present in the chunk data
     */
    private boolean checkNewChunkOverworldOrNether(LevelChunk chunk) {
        var sections = chunk.getSections();
        if (sections.length == 0) return false;
        var firstSection = sections[0];
        Palette<BlockState> firstPalette = firstSection.getStates().data.palette();
        if (isNotLinearOrHashMapPalette(firstPalette)) return false;
        if (firstPalette instanceof LinearPalette<BlockState>) {
            return firstPalette.valueFor(0).getBlock() == Blocks.AIR;
        } else { // HashMapPalette
            // we could iterate through more sections but this is good enough in most cases
            // checking every blockstate is relatively expensive
            for (int i = 0; i < Math.min(sections.length, 3); i++) {
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
     * Similar to Overworld/Nether but we check the biome palette instead
     *
     * New chunks generated in the end will set the first biome palette entry to plains before compaction
     */
    private boolean checkNewChunkEnd(LevelChunk chunk) {
        var sections = chunk.getSections();
        if (sections.length == 0) return false;
        var firstSection = sections[0];
        var biomes = firstSection.getBiomes();
        if (biomes instanceof PalettedContainer<Holder<Biome>> biomesPaletteContainer) {
            Palette<Holder<Biome>> firstPalette = biomesPaletteContainer.data.palette();
            // chunks already generated in the end will not have more than 1 biome present (stored in SingleValuePalette)
            // so just checking the palette size is sufficient
            // but we also check if the first entry is plains to be extra sure
            if (firstPalette.getSize() > 1) {
                Holder<Biome> firstBiome = firstPalette.valueFor(0);
                return firstBiome.unwrapKey().filter(k -> k.equals(Biomes.PLAINS)).isPresent();
            }
        }
        return false;
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

    @EventHandler
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        newChunksCache.handleWorldChange();
        newChunksInverseCache.handleWorldChange();
    }

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        newChunksCache.handleTick();
        newChunksInverseCache.handleTick();
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            new ChunkHighlightProvider(
                this::isHighlighted,
                this::getNewChunksColor
            ));
        newChunksCache.onEnable();
        newChunksInverseCache.onEnable();
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
        newChunksInverseCache.onDisable();
        Globals.drawManager.unregister(this.getClass());
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.paletteNewChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
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

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isInverseNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksInverseCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
