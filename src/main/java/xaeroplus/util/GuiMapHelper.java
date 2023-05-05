package xaeroplus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaeroplus.mixin.client.MixinGuiMapAccessor;

import java.util.Optional;

public class GuiMapHelper {
    public static Optional<GuiMap> getGuiMap() {
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        if (currentScreen instanceof GuiMap) {
            return Optional.of((GuiMap) currentScreen);
        }
        return Optional.empty();
    }
    public static double getCameraX(final GuiMap guiMap) {
        return ((MixinGuiMapAccessor) guiMap).getCameraX();
    }
    public static double getCameraZ(final GuiMap guiMap) {
        return ((MixinGuiMapAccessor)guiMap).getCameraZ();
    }
    public static double getDestScale(final GuiMap guiMap) {
        return ((MixinGuiMapAccessor)guiMap).getDestScale();
    }
    public static int getGuiMapRegionSize(final GuiMap guiMap) {
        // this is intentionally overestimating as we prefer to have a few more chunks than less
        return (int) Math.max((5.0 / getDestScale(guiMap)), 3);
    }

    public static int getGuiMapLoadedDimension() {
        return XaeroWorldMapCore.currentSession.getMapProcessor().getMapWorld().getCurrentDimension().getDimId();
    }
    public static int getCurrentlyViewedDimension() {
        return Shared.customDimensionId;
    }

    public static boolean isGuiMapLoaded() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiMap;
    }

    public static int getGuiMapCenterRegionX(final GuiMap guiMap) {
        return ChunkUtils.coordToRegionCoord(getCameraX(guiMap));
    }

    public static int getGuiMapCenterRegionZ(final GuiMap guiMap) {
        return ChunkUtils.coordToRegionCoord(getCameraZ(guiMap));
    }

}
