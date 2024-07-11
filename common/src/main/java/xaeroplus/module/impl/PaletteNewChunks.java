package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
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
    private int newChunksColor = getColor(255, 0, 0, 100);
    private static final String DATABASE_NAME = "XaeroPlusPaletteNewChunks";

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
    }

    @EventHandler
    public void onChunkData(ChunkDataEvent event) {
        if (event.seenChunk()) return; // never will be newchunk if we've already cached it
        var currentDim = ChunkUtils.getActualDimension();
        try {
            if (currentDim == OVERWORLD || currentDim == NETHER) {
                if (checkNewChunkOverworldOrNether(event.chunk())) {
                    newChunksCache.addHighlight(event.chunk().getPos().x, event.chunk().getPos().z);
                }
            } else if (currentDim == END) {
                if (checkNewChunkEnd(event.chunk())) {
                    newChunksCache.addHighlight(event.chunk().getPos().x, event.chunk().getPos().z);
                }
            }
        } catch (final Exception e) {
            // fall through
        }
    }

    /**
     * MC generates chunks in multiple steps where each step progressively mutates the chunk data
     *
     * when generation is complete there can be block palette entries that no longer
     * have corresponding blockstates present in the chunk data
     *
     * when the MC server writes + reads the chunks to region files it also compacts the palette to save disk space
     * the key is that this compaction occurs _after_ the chunk data is sent to players
     *
     * compacting has 2 effects:
     * 1. palette entries without blockstates present in the chunk are removed
     * 2. the order of ids in the palette changes
     *
     * so we can simply check if the first entry of the lowest section's block palette is air.
     * if the chunk has been generated before it will be removed entirely or its position in the palette changed
     *
     * there is a chance for false negatives depending on features like mineshafts, geodes, etc that generate on the bottom section.
     * but it should be possible to repeat similar checks on other sections to get more accurate results
     */
    private boolean checkNewChunkOverworldOrNether(LevelChunk chunk) {
        var sections = chunk.getSections();
        if (sections.length == 0) return false;
        var firstSection = sections[0];
        Palette<BlockState> firstPalette = firstSection.getStates().data.palette();
        if (firstPalette.getSize() < 1
            || firstPalette instanceof SingleValuePalette<BlockState>
            || firstPalette instanceof GlobalPalette<BlockState>)
            return false;
        try {
            return firstPalette.valueFor(0).getBlock() == Blocks.AIR;
        } catch (final MissingPaletteEntryException e) {
            // fall through
        }
        return false;
    }

    /**
     * Similar concept to Overworld/Nether but here we check the biome palette
     *
     * for some reason end generation sets the first palette entry to plains before compaction
     * so we check if the first entry is the correct void biome
     */
    private boolean checkNewChunkEnd(LevelChunk chunk) {
        var sections = chunk.getSections();
        if (sections.length == 0) return false;
        var firstSection = sections[0];
        var biomes = firstSection.getBiomes();
        if (biomes instanceof PalettedContainer<Holder<Biome>> biomesPaletteContainer) {
            Palette<Holder<Biome>> firstPalette = biomesPaletteContainer.data.palette();
            // singleton palette will never have more than 1 value
            // and we should never have enough entries for a hashmap or global palette
            // so we only care about linear palettes
            if (firstPalette instanceof LinearPalette<Holder<Biome>> linearPalette
                && linearPalette.getSize() > 0) {
                Holder<Biome> firstId = linearPalette.valueFor(0);
                return firstId.unwrapKey().filter(k -> k.equals(Biomes.THE_VOID)).isEmpty();
            }
        }
        return false;
    }

    @EventHandler
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        newChunksCache.handleWorldChange();
    }

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        newChunksCache.handleTick();
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            new ChunkHighlightProvider(
                this::isNewChunk,
                this::getNewChunksColor
            ));
        newChunksCache.onEnable();
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
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

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
