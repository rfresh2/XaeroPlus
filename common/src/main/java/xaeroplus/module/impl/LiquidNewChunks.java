package xaeroplus.module.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xaeroplus.Globals;
import xaeroplus.event.ChunkBlockUpdateEvent;
import xaeroplus.event.ChunkBlocksUpdateEvent;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.render.highlights.SavableHighlightCacheInstance;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.MutableBlockPos;

import java.time.Duration;

import static java.util.Arrays.asList;
import static xaeroplus.util.ChunkUtils.getActualDimension;
import static xaeroplus.util.ColorHelper.getColor;

public class LiquidNewChunks extends Module {
    // chunks where liquid started flowing from source blocks after we loaded it
    private final SavableHighlightCacheInstance newChunksCache = new SavableHighlightCacheInstance("XaeroPlusNewChunks");
    // chunks where liquid was already flowing or flowed when we loaded it
    private final SavableHighlightCacheInstance inverseNewChunksCache = new SavableHighlightCacheInstance("XaeroPlusNewChunksLiquidInverse");
    private final Cache<Long, Byte> seenChunksCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .executor(Globals.cacheRefreshExecutorService.get())
        .expireAfterAccess(Duration.ofMinutes(5))
        .build();
    private boolean renderInverse = false;
    private int newChunksColor = getColor(255, 0, 0, 100);
    private int inverseColor = getColor(0, 255, 0, 100);
    private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };
    private static final ReferenceSet<Block> liquidBlockTypeFilter = new ReferenceOpenHashSet<>(2);
    private static final String inverseDrawFeatureId = "LiquidNewChunksInverse";
    static {
        liquidBlockTypeFilter.addAll(asList(
            Blocks.WATER,
            Blocks.LAVA
        ));
    }

    public void setDiskCache(boolean disk) {
        newChunksCache.setDiskCache(disk, isEnabled());
        inverseNewChunksCache.setDiskCache(disk, isEnabled());
    }

    @EventHandler
    public void onMultiBlockUpdate(final ChunkBlocksUpdateEvent event) {
        var level = mc.level;
        if (level == null || mc.levelRenderer.viewArea == null) return;
        event.packet().runUpdates((pos, state) -> {
            handleBlockUpdate(level, pos, state);
        });
    }

    @EventHandler
    public void onBlockUpdate(final ChunkBlockUpdateEvent event) {
        var level = mc.level;
        if (level == null || mc.levelRenderer.viewArea == null) return;
        handleBlockUpdate(level, event.packet().getPos(), event.packet().getBlockState());
    }

    private void handleBlockUpdate(Level level, BlockPos pos, BlockState state) {
        if (!state.getFluidState().isEmpty() && !state.getFluidState().isSource()) {
            int chunkX = ChunkUtils.posToChunkPos(pos.getX());
            int chunkZ = ChunkUtils.posToChunkPos(pos.getZ());
            if (inverseNewChunksCache.get().isHighlighted(chunkX, chunkZ, ChunkUtils.getActualDimension())) return;
            if (newChunksCache.get().isHighlighted(chunkX, chunkZ, getActualDimension())) return;
            final int srcX = pos.getX();
            final int srcY = pos.getY();
            final int srcZ = pos.getZ();
            MutableBlockPos bp = new MutableBlockPos(srcX, srcY, srcZ);
            for (int i = 0; i < searchDirs.length; i++) {
                final Direction dir = searchDirs[i];
                bp.setPos(srcX + dir.getStepX(), srcY + dir.getStepY(), srcZ + dir.getStepZ());
                if (level.getBlockState(bp).getFluidState().isSource()) {
                    newChunksCache.get().addHighlight(chunkX, chunkZ);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onChunkData(final ChunkDataEvent event) {
        var level = mc.level;
        if (level == null || mc.levelRenderer.viewArea == null) return;
        var chunk = event.chunk();
        var chunkPos = chunk.getPos();
        long chunkLong = ChunkUtils.chunkPosToLong(chunkPos);

        // only scan the first time we see the chunk
        // mc server can send us the same chunk multiple times in certain cases
        // or the player could have travelled back into chunks we just saw
        if (seenChunksCache.getIfPresent(chunkLong) != null) return;
        seenChunksCache.put(chunkLong, Byte.MAX_VALUE);
        if (newChunksCache.get().isHighlighted(chunkPos.x, chunkPos.z, getActualDimension())) return;
        if (inverseNewChunksCache.get().isHighlighted(chunkPos.x, chunkPos.z, getActualDimension())) return;

        ChunkScanner.chunkScanBlockstatePredicate(chunk, liquidBlockTypeFilter, (c, state, relX, y, relZ) -> {
            int x = ChunkUtils.chunkCoordToCoord(c.getPos().x) + relX;
            int z = ChunkUtils.chunkCoordToCoord(c.getPos().z) + relZ;

            var fluid = state.getFluidState();
            if (!fluid.isEmpty() && !fluid.isSource()) {
                if (fluid.getAmount() < 2) {
                    inverseNewChunksCache.get().addHighlight(c.getPos().x, chunk.getPos().z);
                    return true;
                }
                boolean foundColumn = true;
                for (int i = 1; i <= 5; i++) {
                    var aboveState = chunk.getFluidState(x, y + i, z);
                    if (aboveState.isEmpty() || aboveState.isSource()) {
                        foundColumn = false;
                        break;
                    }
                }
                if (foundColumn) {
                    inverseNewChunksCache.get().addHighlight(c.getPos().x, c.getPos().z);
                    return true;
                }
            }
            return false;
        }, level.getMinBuildHeight());
    }

    @EventHandler
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        seenChunksCache.invalidateAll(); // side effect - switching dimensions resets our state
    }

    public synchronized void setInverseRenderEnabled(final boolean b) {
        this.renderInverse = b;
        if (this.renderInverse && this.isEnabled()) {
            registerInverseChunkHighlightProvider();
        } else {
            Globals.drawManager.unregisterChunkHighlightProvider(inverseDrawFeatureId);
        }
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            this::getNewChunkHighlightsSnapshot,
            this::getNewChunksColor);
        if (renderInverse) {
            registerInverseChunkHighlightProvider();
        }
        newChunksCache.onEnable();
        inverseNewChunksCache.onEnable();
    }

    private void registerInverseChunkHighlightProvider() {
        Globals.drawManager.registerChunkHighlightProvider(
            inverseDrawFeatureId,
            this::getInverseNewChunkHighlightsSnapshot,
            this::getInverseColor);
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
        inverseNewChunksCache.onDisable();
        Globals.drawManager.unregisterChunkHighlightProvider(this.getClass());
        Globals.drawManager.unregisterChunkHighlightProvider(inverseDrawFeatureId);
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    private int getInverseColor() {
        return this.inverseColor;
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.liquidNewChunksAlphaSetting.getValue());
    }

    public void setInverseRgbColor(final int color) {
        inverseColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.liquidNewChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
        inverseColor = ColorHelper.getColorWithAlpha(inverseColor, (int) (a));
    }

    public LongList getNewChunkHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return newChunksCache.get().getHighlightsSnapshot(dimension);
    }

    public LongList getInverseNewChunkHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return inverseNewChunksCache.get().getHighlightsSnapshot(dimension);
    }

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksCache.get().isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isInverseNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return inverseNewChunksCache.get().isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
