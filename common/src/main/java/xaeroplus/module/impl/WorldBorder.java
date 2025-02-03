package xaeroplus.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.Line;
import xaeroplus.module.Module;
import xaeroplus.util.ColorHelper;

import java.util.Collections;
import java.util.List;

public class WorldBorder extends Module {
    private final int color = ColorHelper.getColor(0, 255, 255, 204);

    @Override
    public void onEnable() {
        Globals.drawManager.registry().registerLineProvider(
            this.getClass().getName(),
            this::getLines,
            this::getColor,
            this::getLineWidth,
            1000
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregisterLineProvider(this.getClass().getName());
    }

    List<Line> getLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return Collections.emptyList();
        if (level.dimension() != dimension) return Collections.emptyList();
        var worldBorder = level.getWorldBorder();
        int minX = Mth.floor(worldBorder.getMinX());
        int minZ = Mth.floor(worldBorder.getMinZ());
        int maxX = Mth.floor(worldBorder.getMaxX());
        int maxZ = Mth.floor(worldBorder.getMaxZ());
        return List.of(
            new Line(minX, minZ, maxX, minZ),
            new Line(maxX, minZ, maxX, maxZ),
            new Line(maxX, maxZ, minX, maxZ),
            new Line(minX, minZ, minX, maxZ)
        );
    }

    int getColor() {
        return color;
    }

    float getLineWidth() {
        return 0.1f;
    }
}
