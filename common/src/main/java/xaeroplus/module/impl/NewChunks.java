package xaeroplus.module.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.*;
import xaeroplus.feature.render.ChunkHighlightProvider;
import xaeroplus.feature.render.ColorHelper;
import xaeroplus.feature.render.highlights.ChunkHighlightCache;
import xaeroplus.feature.render.highlights.ChunkHighlightLocalCache;
import xaeroplus.feature.render.highlights.ChunkHighlightSavingCache;
import xaeroplus.mixin.client.mc.AccessorWorldRenderer;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static net.minecraft.world.level.Level.*;
import static xaeroplus.feature.render.ColorHelper.getColor;
import static xaeroplus.util.ChunkUtils.getActualDimension;

public class NewChunks extends Module {
    // chunks where liquid has not flowed from source blocks
    private ChunkHighlightCache newChunksCache = new ChunkHighlightLocalCache();
    // chunks where liquid sources have already flowed
    private final Cache<Long, Byte> oldChunksCache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();
    private int newChunksColor = getColor(255, 0, 0, 100);
    private final Minecraft mc = Minecraft.getInstance();
    private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };
    private static final String DATABASE_NAME = "XaeroPlusNewChunks";

    public void setNewChunksCache(boolean disk) {
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
            XaeroPlus.LOGGER.error("Error closing new chunks cache", e);
        }
    }

    @EventHandler
    public void onMultiBlockUpdate(final ChunkBlocksUpdateEvent event) {
        if (mc.level == null || ((AccessorWorldRenderer) mc.levelRenderer).getChunks() == null) return;
        event.packet().runUpdates((pos, state) -> {
            if (!state.getFluidState().isEmpty() && !state.getFluidState().isSource()) {
                ChunkPos chunkPos = new ChunkPos(pos);
                for (Direction dir: searchDirs) {
                    if (mc.level.getBlockState(pos.relative(dir)).getFluidState().isSource()
                        && oldChunksCache.getIfPresent(ChunkUtils.chunkPosToLong(chunkPos)) == null) {
                        newChunksCache.addHighlight(chunkPos.x, chunkPos.z);
                        return;
                    }
                }
            }
        });
    }

    @EventHandler
    public void onBlockUpdate(final ChunkBlockUpdateEvent event) {
        if (mc.level == null || ((AccessorWorldRenderer) mc.levelRenderer).getChunks() == null) return;
        var packet = event.packet();
        if (!packet.getBlockState().getFluidState().isEmpty() && !packet.getBlockState().getFluidState().isSource()) {
            final int chunkX = ChunkUtils.posToChunkPos(packet.getPos().getX());
            final int chunkZ = ChunkUtils.posToChunkPos(packet.getPos().getZ());
            final long chunkPosLong = ChunkUtils.chunkPosToLong(chunkX, chunkZ);
            for (Direction dir: searchDirs) {
                if (mc.level.getBlockState(packet.getPos().relative(dir)).getFluidState().isSource()
                    && oldChunksCache.getIfPresent(chunkPosLong) == null) {
                    newChunksCache.addHighlight(chunkX, chunkZ);
                    return;
                }
            }
        }
    }

    private static final ReferenceSet<Block> liquidBlockTypeFilter = new ReferenceOpenHashSet<>(2);
    static {
        liquidBlockTypeFilter.addAll(asList(
            Blocks.WATER,
            Blocks.LAVA
        ));
    }
    @EventHandler
    public void onChunkData(final ChunkDataEvent event) {
        if (mc.level == null || ((AccessorWorldRenderer) mc.levelRenderer).getChunks() == null) return;
        var chunk = event.chunk();
        var chunkPos = chunk.getPos();
        if (newChunksCache.isHighlighted(chunkPos.x, chunkPos.z, getActualDimension())) return;
        ChunkScanner.chunkScanBlockstatePredicate(chunk, liquidBlockTypeFilter, state -> {
            var fluid = state.getFluidState();
            if (!fluid.isEmpty() && !fluid.isSource()) {
                oldChunksCache.put(ChunkUtils.chunkPosToLong(chunkPos), (byte) 0);
                return true;
            }
            return false;
        }, mc.level.getMinBuildHeight());
    }

    @EventHandler
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getValue()) {
            if (inUnknownDimension() && newChunksCache instanceof ChunkHighlightSavingCache) {
                XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.setValue(false);
                XaeroPlus.LOGGER.warn("Entered unknown dimension with saving cache on, disabling disk saving");
            }
        }
        newChunksCache.handleWorldChange();
    }

    public boolean inUnknownDimension() {
        final ResourceKey<Level> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
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
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.newChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
    }

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
