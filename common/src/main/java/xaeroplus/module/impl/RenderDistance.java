package xaeroplus.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.Line;
import xaeroplus.module.Module;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.Collections;
import java.util.List;

import static xaeroplus.util.ChunkUtils.chunkCoordToCoord;
import static xaeroplus.util.ChunkUtils.coordToChunkCoord;

public class RenderDistance extends Module {
    private final int color = ColorHelper.getColor(255, 255, 0, 204);

    @Override
    public void onEnable() {
        Globals.drawManager.registry().registerLineProvider(
            this.getClass().getName(),
            this::getLines,
            this::getColor,
            this::getLineWidth,
            50
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregisterLineProvider(this.getClass().getName());
    }

    List<Line> getLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return Collections.emptyList();
        if (dimension != ChunkUtils.getActualDimension()) return Collections.emptyList();
        final int viewDistance = mc.options.serverRenderDistance;
        final int width = viewDistance * 2 + 1;
        final int middleChunkX = coordToChunkCoord(Mth.floor(player.getX()));
        final int middleChunkZ = coordToChunkCoord(Mth.floor(player.getZ()));
        final int chunkLeftX = middleChunkX - (width / 2);
        final int chunkTopZ = middleChunkZ - (width / 2);
        final int chunkRightX = chunkLeftX + width;
        final int chunkBottomZ = chunkTopZ + width;
        final int minBlockX = chunkCoordToCoord(chunkLeftX);
        final int minBlockZ = chunkCoordToCoord(chunkTopZ);
        final int maxBlockX = chunkCoordToCoord(chunkRightX);
        final int maxBlockZ = chunkCoordToCoord(chunkBottomZ);
        return List.of(
            new Line(minBlockX, minBlockZ, maxBlockX, minBlockZ),
            new Line(maxBlockX, minBlockZ, maxBlockX, maxBlockZ),
            new Line(maxBlockX, maxBlockZ, minBlockX, maxBlockZ),
            new Line(minBlockX, minBlockZ, minBlockX, maxBlockZ)
        );
    }

    int getColor() {
        return color;
    }

    float getLineWidth() {
        return 0.1f;
    }
}
