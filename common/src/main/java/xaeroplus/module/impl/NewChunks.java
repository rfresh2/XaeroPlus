package xaeroplus.module.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
import xaeroplus.util.MutableBlockPos;

import java.time.Duration;

import static java.util.Arrays.asList;
import static net.minecraft.world.level.Level.*;
import static xaeroplus.feature.render.ColorHelper.getColor;
import static xaeroplus.util.ChunkUtils.getActualDimension;

public class NewChunks extends Module {
    // chunks where liquid started flowing from source blocks after we loaded it
    private ChunkHighlightCache newChunksCache = new ChunkHighlightLocalCache();
    // chunks where liquid was already flowing or flowed when we loaded it
    // todo: setting to save these to a db?
    private final ChunkHighlightLocalCache inverseNewChunksCache = new ChunkHighlightLocalCache();
    private boolean renderInverse = false;
    private int newChunksColor = getColor(255, 0, 0, 100);
    private int inverseColor = getColor(0, 255, 0, 100);
    private final Minecraft mc = Minecraft.getInstance();
    private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };
    private static final String DATABASE_NAME = "XaeroPlusNewChunks";
    private static final ReferenceSet<Block> liquidBlockTypeFilter = new ReferenceOpenHashSet<>(2);
    static {
        liquidBlockTypeFilter.addAll(asList(
            Blocks.WATER,
            Blocks.LAVA
        ));
    }

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
        var level = mc.level;
        if (level == null || ((AccessorWorldRenderer) mc.levelRenderer).getChunks() == null) return;
        event.packet().runUpdates((pos, state) -> {
            handleBlockUpdate(level, pos, state);
        });
    }

    @EventHandler
    public void onBlockUpdate(final ChunkBlockUpdateEvent event) {
        var level = mc.level;
        if (level == null || ((AccessorWorldRenderer) mc.levelRenderer).getChunks() == null) return;
        handleBlockUpdate(level, event.packet().getPos(), event.packet().getBlockState());
    }

    private void handleBlockUpdate(Level level, BlockPos pos, BlockState state) {
        if (!state.getFluidState().isEmpty() && !state.getFluidState().isSource()) {
            int chunkX = ChunkUtils.posToChunkPos(pos.getX());
            int chunkZ = ChunkUtils.posToChunkPos(pos.getZ());
            if (inverseNewChunksCache.isHighlighted(chunkX, chunkZ, ChunkUtils.getActualDimension())) return;
            final int srcX = pos.getX();
            final int srcY = pos.getY();
            final int srcZ = pos.getZ();
            MutableBlockPos bp = new MutableBlockPos(srcX, srcY, srcZ);
            for (int i = 0; i < searchDirs.length; i++) {
                final Direction dir = searchDirs[i];
                bp.setPos(srcX + dir.getStepX(), srcY + dir.getStepY(), srcZ + dir.getStepZ());
                if (level.getBlockState(bp).getFluidState().isSource()) {
                    newChunksCache.addHighlight(chunkX, chunkZ);
                    return;
                }
            }
        }
    }


    final Cache<Long, Byte> seenChunksCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .executor(Globals.cacheRefreshExecutorService.get())
        .expireAfterAccess(Duration.ofMinutes(5))
        .build();

    @EventHandler
    public void onChunkData(final ChunkDataEvent event) {
        var level = mc.level;
        if (level == null || ((AccessorWorldRenderer) mc.levelRenderer).getChunks() == null) return;
        var chunk = event.chunk();
        var chunkPos = chunk.getPos();
        long chunkLong = ChunkUtils.chunkPosToLong(chunkPos);

        // only scan the first time we see the chunk
        // mc server can send us the same chunk multiple times in certain cases
        // or the player could have travelled back into chunks we just saw
        if (seenChunksCache.getIfPresent(chunkLong) != null) return;
        seenChunksCache.put(chunkLong, Byte.MAX_VALUE);

        if (newChunksCache.isHighlighted(chunkPos.x, chunkPos.z, getActualDimension())) return;

        ChunkScanner.chunkVisitor(chunk, (c, state, relX, y, relZ) -> {
            int x = ChunkUtils.chunkCoordToCoord(c.getPos().x) + relX;
            int z = ChunkUtils.chunkCoordToCoord(c.getPos().z) + relZ;

            var fluid = state.getFluidState();
            if (!fluid.isEmpty() && !fluid.isSource()) {
                if (fluid.getAmount() < 2) {
                    inverseNewChunksCache.addHighlight(c.getPos().x, chunk.getPos().z);
                    return true;
                }
                boolean foundColumn = true;
                for (int i = 1; i <= 5; i++) {
                    var aboveState = chunk.getFluidState(x, y + i, z);
                    if (aboveState.isEmpty()) {
                        foundColumn = false;
                        break;
                    }
                }
                if (foundColumn) {
                    inverseNewChunksCache.addHighlight(c.getPos().x, c.getPos().z);
                    return true;
                }
            }
            return false;
        });
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
        inverseNewChunksCache.handleWorldChange();
        inverseNewChunksCache.reset();
        seenChunksCache.invalidateAll(); // side effect - switching dimensions resets our state
    }

    public boolean inUnknownDimension() {
        final ResourceKey<Level> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
    }

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        newChunksCache.handleTick();
        inverseNewChunksCache.handleTick();
    }

    public synchronized void setInverseRenderEnabled(final boolean b) {
        this.renderInverse = b;
        if (this.renderInverse && this.isEnabled()) {
            registerInverseChunkHighlightProvider();
        } else {
            Globals.drawManager.unregister(InverseRenderHolderClass.class);
        }
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            new ChunkHighlightProvider(
                this::isNewChunk,
                this::getNewChunksColor
            ));
        if (renderInverse) {
            registerInverseChunkHighlightProvider();
        }
        newChunksCache.onEnable();
        inverseNewChunksCache.onEnable();
    }

    static class InverseRenderHolderClass { }
    private void registerInverseChunkHighlightProvider() {
        Globals.drawManager.registerChunkHighlightProvider(
            InverseRenderHolderClass.class,
            new ChunkHighlightProvider(
                this::isInverseNewChunk,
                this::getInverseColor
            )
        );
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
        inverseNewChunksCache.onDisable();
        Globals.drawManager.unregister(this.getClass());
        Globals.drawManager.unregister(InverseRenderHolderClass.class);
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    private int getInverseColor() {
        return this.inverseColor;
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.newChunksAlphaSetting.getValue());
    }

    public void setInverseRgbColor(final int color) {
        inverseColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.newChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
        inverseColor = ColorHelper.getColorWithAlpha(inverseColor, (int) (a));
    }

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isInverseNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return inverseNewChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
