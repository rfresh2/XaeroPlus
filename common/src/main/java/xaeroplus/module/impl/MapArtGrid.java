package xaeroplus.module.impl;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.Line;
import xaeroplus.module.Module;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.List;

public class MapArtGrid extends Module {
    private int alpha = 255;
    private int color = ColorHelper.getColor(255, 0, 0, alpha);
    private float lineWidth = 0.1f;

    @Override
    public void onEnable() {
        Globals.drawManager.registry().registerLineProvider(
            getClass().getSimpleName(),
            this::getLines,
            this::getColor,
            this::getLineWidth,
            1000
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregisterLineProvider(getClass().getSimpleName());
    }

    public List<Line> getLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        ArrayList<Line> lines = new ArrayList<>();
        int padding = 10000;
        int minBlockX = ChunkUtils.regionCoordToCoord(windowRegionX - windowRegionSize) - padding;
        int minBlockZ = ChunkUtils.regionCoordToCoord(windowRegionZ - windowRegionSize) - padding;
        int maxBlockX = ChunkUtils.regionCoordToCoord(windowRegionX + windowRegionSize) + padding;
        int maxBlockZ = ChunkUtils.regionCoordToCoord(windowRegionZ + windowRegionSize) + padding;

        // horizontal and vertical lines every 128 blocks: https://minecraft.wiki/w/Map#Usage
        int minXGridPos = minBlockX - (minBlockX % 128) - 64;
        int minZGridPos = minBlockZ - (minBlockZ % 128) - 64;
        int maxXGridPos = maxBlockX - (maxBlockX % 128) + 64;
        int maxZGridPos = maxBlockZ - (maxBlockZ % 128) + 64;

        for (int x = minXGridPos; x <= maxXGridPos; x += 128) {
            lines.add(new Line(x, minBlockZ, x, maxBlockZ));
        }
        for (int z = minZGridPos; z <= maxZGridPos; z += 128) {
            lines.add(new Line(minBlockX, z, maxBlockX, z));
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
}
