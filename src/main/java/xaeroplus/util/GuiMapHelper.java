package xaeroplus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.*;
import xaero.map.world.MapDimension;
import xaeroplus.mixin.client.AccessorGuiCaveModeOptions;
import xaeroplus.mixin.client.MixinGuiMapAccessor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class GuiMapHelper {
    public static Optional<GuiMap> getGuiMap() {
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        if (currentScreen instanceof GuiMap) {
            return Optional.of((GuiMap) currentScreen);
        } else if (currentScreen instanceof GuiSettings && ((GuiSettings) currentScreen).parent instanceof GuiMap) {
            return Optional.of((GuiMap) ((GuiSettings) currentScreen).parent);
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

    // workaround mixin bug where we can't use accessors inside mixin classes
    public static void updateCaveModeOptions(final GuiCaveModeOptions guiCaveModeOptions, MapDimension newDimension, final List<GuiButton> buttonList) {
        AccessorGuiCaveModeOptions options = (AccessorGuiCaveModeOptions) guiCaveModeOptions;
        options.setDimension(newDimension);
        for (GuiButton button : buttonList) {
            if (!(button instanceof TooltipButton)) continue;
            final TooltipButton tooltipButton = (TooltipButton) button;
            final Supplier<CursorBox> xaeroWmTooltipSupplier = tooltipButton.getTooltip();
            if (xaeroWmTooltipSupplier == null) continue;
            final CursorBox cursorBox = xaeroWmTooltipSupplier.get();
            if (cursorBox == null) continue;
            final String code = cursorBox.getFullCode();
            if (Objects.equals(code, "gui.xaero_wm_box_cave_mode_type")) {
                button.displayString = options.invokeCaveModeTypeButtonMessage().getFormattedText();
                break;
            }
        }
    }

}
