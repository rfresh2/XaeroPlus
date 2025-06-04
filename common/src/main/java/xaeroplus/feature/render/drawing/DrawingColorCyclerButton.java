package xaeroplus.feature.render.drawing;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import xaero.map.gui.CursorBox;
import xaero.map.gui.GuiTexturedButton;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.Drawing;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class DrawingColorCyclerButton extends GuiTexturedButton {
    protected final Drawing.DrawingColorCycler drawingColorCycler;
    protected final IntSupplier alphaSupplier;
    public DrawingColorCyclerButton(final int x, final int y, final ResourceLocation texture, final Supplier<CursorBox> tooltip, final Drawing.DrawingColorCycler drawingColorCycler) {
        super(
            x, y,
            20, 20,
            82, 0,
            16, 16,
            texture,
            (button) -> ModuleManager.getModule(Drawing.class).getDrawingColorCycler().next(),
            tooltip);
        this.drawingColorCycler = drawingColorCycler;
        this.alphaSupplier = () -> 220;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int minX = this.getX() + 8;
        int minY = this.getY() + 7;
        if (this.isActive() && this.isHovered()) {
            minY--;
        }
        int maxX = minX + 6;
        int maxY = minY + 6;
        guiGraphics.fill(minX, minY, maxX, maxY, drawingColorCycler.getColorInt(alphaSupplier.getAsInt()));
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }
}
