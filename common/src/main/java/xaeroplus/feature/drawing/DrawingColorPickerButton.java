package xaeroplus.feature.drawing;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.gui.TooltipButton;
import xaeroplus.util.Color;

import java.util.Objects;
import java.util.function.Supplier;

public class DrawingColorPickerButton extends TooltipButton {
    private final Supplier<Color> colorSupplier;

    public DrawingColorPickerButton(
        final int x,
        final int y,
        final Supplier<Tooltip> tooltip,
        final Supplier<Color> colorSupplier,
        final Button.OnPress onPress
    ) {
        super(x, y, 20, 20, Component.empty(), onPress, tooltip);
        this.colorSupplier = Objects.requireNonNull(colorSupplier);
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        var minX = getX() + 6;
        var minY = getY() + 6;
        if (isActive() && isHovered()) {
            minY--;
        }
        guiGraphics.fill(minX, minY, minX + 10, minY + 10, colorSupplier.get().getInt());
    }
}
