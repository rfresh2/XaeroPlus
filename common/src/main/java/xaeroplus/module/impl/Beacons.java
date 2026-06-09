package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import xaeroplus.Globals;
import xaeroplus.event.ChunkBlockUpdateEvent;
import xaeroplus.event.ChunkBlocksUpdateEvent;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.event.Phase;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.feature.render.line.Line;
import xaeroplus.module.Module;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.WeakHashMap;

public class Beacons extends Module {
    private int alpha = 255;
    private float lineWidth = 0.1f;
    private final WeakHashMap<BeaconBlockEntity, Boolean> beaconBlockEntityCache = new WeakHashMap<>();

    @Override
    public void onEnable() {
        Globals.drawManager.registry().register(
            DrawFeatureFactory.multiColorLines(
                "Beacons",
                this::getLines,
                this::getColor,
                this::getLineWidth,
                250
            )
        );
        searchAllLoadedChunks();
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregister("Beacons");
        beaconBlockEntityCache.clear();
    }

    @EventHandler
    public void onChunkData(ChunkDataEvent event) {
        if (event.seenChunk()) return;
        searchChunkForBeacons(event.chunk());
    }

    @EventHandler
    public void onChunkBlockUpdatePre(ChunkBlockUpdateEvent event) {
        if (event.phase() == Phase.PRE) {
            var existingBlockEntity = mc.level.getBlockEntity(event.packet().getPos());
            if (existingBlockEntity instanceof BeaconBlockEntity bbe) {
                beaconBlockEntityCache.remove(bbe);
            }
        } else if (event.phase() == Phase.POST) {
            var newBlockEntity = mc.level.getBlockEntity(event.packet().getPos());
            if (newBlockEntity instanceof BeaconBlockEntity bbe) {
                beaconBlockEntityCache.put(bbe, true);
            }
        }
    }

    @EventHandler
    public void onChunkBlocksUpdate(ChunkBlocksUpdateEvent event) {
        if (event.phase() == Phase.PRE) {
            event.packet().runUpdates((pos, newState) -> {
                var existingBlockEntity = mc.level.getBlockEntity(pos);
                if (existingBlockEntity instanceof BeaconBlockEntity bbe) {
                    beaconBlockEntityCache.remove(bbe);
                }
            });
        } else if (event.phase() == Phase.POST) {
            event.packet().runUpdates((pos, newState) -> {
                var newBlockEntity = mc.level.getBlockEntity(pos);
                if (newBlockEntity instanceof BeaconBlockEntity bbe) {
                    beaconBlockEntityCache.put(bbe, true);
                }
            });
        }
    }


    private void searchAllLoadedChunks() {
        if (mc.level == null) return;
        final int renderDist = mc.options.renderDistance().get();
        final int xMin = ChunkUtils.actualPlayerChunkX() - renderDist;
        final int xMax = ChunkUtils.actualPlayerChunkX() + renderDist;
        final int zMin = ChunkUtils.actualPlayerChunkZ() - renderDist;
        final int zMax = ChunkUtils.actualPlayerChunkZ() + renderDist;
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                var chunk = mc.level.getChunkSource().getChunk(x, z, false);
                if (chunk instanceof EmptyLevelChunk || chunk == null) continue;
                searchChunkForBeacons(chunk);
            }
        }
    }

    private void getLinesFromBeaconBlockEntity(BeaconBlockEntity beaconBlockEntity, Object2IntMap<Line> linesCollector) {
        var levels = beaconBlockEntity.levels;
        if (levels > 0) {
            int lineColor = ColorHelper.getColor(255, 255, 255, 255);
            var beamSections = beaconBlockEntity.getBeamSections();
            if (!beamSections.isEmpty()) {
                var beaconColorInt = beamSections.get(beamSections.size()-1).getColor();
                lineColor = ColorHelper.getColor(ColorHelper.getIntR(beaconColorInt), ColorHelper.getIntG(beaconColorInt), ColorHelper.getIntB(beaconColorInt), 255);
            }
            var d = levels * 10 + 10;
            var x = beaconBlockEntity.getBlockPos().getX();
            var z = beaconBlockEntity.getBlockPos().getZ();
            var minX = x - d;
            var maxX = x + d;
            var minZ = z - d;
            var maxZ = z + d;
            linesCollector.put(new Line(minX, minZ, maxX, minZ), lineColor);
            linesCollector.put(new Line(minX, maxZ, maxX, maxZ), lineColor);
            linesCollector.put(new Line(minX, minZ, minX, maxZ), lineColor);
            linesCollector.put(new Line(maxX, minZ, maxX, maxZ), lineColor);
        }
    }

    private final ReferenceSet<Block> BEACON_BLOCK_SET = ReferenceSet.of(Blocks.BEACON);

    private void searchChunkForBeacons(final ChunkAccess chunk) {
        var level = mc.level;
        ChunkScanner.chunkScanBlockstatePredicate(chunk, BEACON_BLOCK_SET, (c, state, relX, y, relZ) -> {
            var x = ChunkUtils.chunkCoordToCoord(c.getPos().x) + relX;
            var z = ChunkUtils.chunkCoordToCoord(c.getPos().z) + relZ;
            if (!state.hasBlockEntity()) return false;
            var blockEntity = c.getBlockEntity(new BlockPos(x, y, z));
            if (blockEntity == null) return false;
            if (blockEntity instanceof BeaconBlockEntity beaconBlockEntity) {
                beaconBlockEntityCache.put(beaconBlockEntity, true);
            }
            return false;
        }, level.getMinBuildHeight());
    }

    Object2IntMap<Line> getLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        if (dimension != ChunkUtils.getActualDimension()) return Object2IntMaps.emptyMap();
        var level = mc.level;
        if (level == null || mc.levelRenderer.viewArea == null) return Object2IntMaps.emptyMap();
        var lines = new Object2IntOpenHashMap<Line>();
        for (var it = beaconBlockEntityCache.keySet().iterator(); it.hasNext(); ) {
            var beaconBlockEntity = it.next();
            // there is some delay between the beacon getting gc'd and it being removed
            // and our referencing of it here constantly will also likely reduce gc chance
            // so just check and remove proactively
            if (beaconBlockEntity.isRemoved()) {
                it.remove();
                continue;
            }
            getLinesFromBeaconBlockEntity(beaconBlockEntity, lines);
        }
        return lines;
    }

    int getColor(Line line, int color) {
        return ColorHelper.getColorWithAlpha(color, this.alpha);
    }

    float getLineWidth() {
        return this.lineWidth;
    }
}
