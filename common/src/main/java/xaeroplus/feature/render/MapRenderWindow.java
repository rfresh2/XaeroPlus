package xaeroplus.feature.render;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.util.ChunkUtils;

import static xaeroplus.util.GuiMapHelper.*;

public record MapRenderWindow(int windowX, int windowZ, int windowSize, ResourceKey<Level> dimension) {
    public static MapRenderWindow resolveCurrent() {
        final int windowX;
        final int windowZ;
        final int windowSize;
        var guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            var guiMap = guiMapOptional.get();
            windowX = getGuiMapCenterRegionX(guiMap);
            windowZ = getGuiMapCenterRegionZ(guiMap);
            windowSize = getGuiMapRegionSize(guiMap);
        } else {
            windowX = ChunkUtils.getPlayerRegionX();
            windowZ = ChunkUtils.getPlayerRegionZ();
            windowSize = Math.max(3, Globals.minimapScaleMultiplier);
        }
        return new MapRenderWindow(windowX, windowZ, windowSize, Globals.getCurrentDimensionId());
    }
}
