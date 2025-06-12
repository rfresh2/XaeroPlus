package xaeroplus.feature.render.drawing;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import xaero.map.gui.CursorBox;
import xaero.map.gui.TooltipButton;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.Drawing;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class DrawingColorCyclerButton extends TooltipButton {
    protected final Drawing.DrawingColorCycler drawingColorCycler;
    protected final IntSupplier alphaSupplier;
    public DrawingColorCyclerButton(final int x, final int y, final Supplier<CursorBox> tooltip, final Drawing.DrawingColorCycler drawingColorCycler) {
        super(
            x, y,
            20, 20,
            Component.literal(""),
            (button) -> ModuleManager.getModule(Drawing.class).getDrawingColorCycler().next(),
            tooltip
        );
        this.drawingColorCycler = drawingColorCycler;
        this.alphaSupplier = () -> 220;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int minX = this.getX() + 6;
        int minY = this.getY() + 4;
        if (this.isActive() && this.isHovered()) {
            minY--;
        }
        int maxX = minX + 9;
        int maxY = minY + 9;
        guiGraphics.fill(minX, minY, maxX, maxY, drawingColorCycler.getColorInt(alphaSupplier.getAsInt()));
    }
}
