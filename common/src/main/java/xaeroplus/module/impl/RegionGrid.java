package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.Module;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.GuiMapHelper;

import java.util.ArrayList;
import java.util.List;

public class RegionGrid extends Module {
    private int alpha = 255;
    private int color = ColorHelper.getColor(255, 0, 0, alpha);
    private float lineWidth = 0.1f;
    private boolean textEnabled = false;

    @Override
    public void onEnable() {
        Globals.drawManager.registry().register(
            DrawFeatureFactory.lines(
                "RegionGrid",
                this::getLines,
                this::getColor,
                this::getLineWidth,
                1000
            )
        );
        Globals.drawManager.registry().register(
            DrawFeatureFactory.text(
                "RegionGridText",
                this::getText,
                250
            )
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregister("RegionGrid");
        Globals.drawManager.registry().unregister("RegionGridText");
    }

    private Long2ObjectMap<Text> getText(int windowRegionX, int windowRegionZ, int windowRegionSize, ResourceKey<Level> dimension) {
        if (!textEnabled) return Long2ObjectMaps.emptyMap();
        var guiMap = GuiMapHelper.getGuiMap();
        if (guiMap.isPresent()) {
            var scale = GuiMapHelper.getDestScale(guiMap.get());
            // unreadable at low zooms and may crash game
            if (scale < 0.1) return Long2ObjectMaps.emptyMap();
        }
        Long2ObjectMap<Text> map = new Long2ObjectOpenHashMap<>();
        int padding = ChunkUtils.coordToRegionCoord(10000);
        int minRegionX = windowRegionX - windowRegionSize - padding;
        int minRegionZ = windowRegionZ - windowRegionSize - padding;
        int maxRegionX = windowRegionX + windowRegionSize + padding;
        int maxRegionZ = windowRegionZ + windowRegionSize + padding;

        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                int centerChunkX = ChunkUtils.regionCoordToChunkCoord(regionX) + 16;
                int centerChunkZ = ChunkUtils.regionCoordToChunkCoord(regionZ) + 16;
                int blockX = ChunkUtils.chunkCoordToCoord(centerChunkX) + 8;
                int blockZ = ChunkUtils.chunkCoordToCoord(centerChunkZ) + 8;
                String regionText = "r." + regionX + "." + regionZ;
                map.put(ChunkUtils.chunkPosToLong(blockX, blockZ), new Text(regionText, blockX, blockZ, ColorHelper.getColor(255, 255, 255, alpha), 1f));
            }
        }
        return map;
    }

    public List<Line> getLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        ArrayList<Line> lines = new ArrayList<>();
        int padding = ChunkUtils.coordToRegionCoord(10000);
        int minRegionX = windowRegionX - windowRegionSize - padding;
        int minRegionZ = windowRegionZ - windowRegionSize - padding;
        int maxRegionX = windowRegionX + windowRegionSize + padding;
        int maxRegionZ = windowRegionZ + windowRegionSize + padding;
        int minBlockX = ChunkUtils.regionCoordToCoord(minRegionX);
        int minBlockZ = ChunkUtils.regionCoordToCoord(minRegionZ);
        int maxBlockX = ChunkUtils.regionCoordToCoord(maxRegionX);
        int maxBlockZ = ChunkUtils.regionCoordToCoord(maxRegionZ);

        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            int blockX = ChunkUtils.regionCoordToCoord(regionX);
            lines.add(new Line(blockX, minBlockZ, blockX, maxBlockZ));
        }

        for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
            int blockZ = ChunkUtils.regionCoordToCoord(regionZ);
            lines.add(new Line(minBlockX, blockZ, maxBlockX, blockZ));
        }
        return lines;
    }

    public int getColor() {
        return color;
    }

    public void setRgbColor(final int color) {
        this.color = ColorHelper.getColorWithAlpha(color, alpha);
    }

    public void setAlpha(final int alpha) {
        this.alpha = alpha;
        this.color = ColorHelper.getColorWithAlpha(color, alpha);
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(final float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public void setTextEnabled(final boolean b) {
        this.textEnabled = b;
    }
}
