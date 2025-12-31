package xaeroplus.feature.drawing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.gui.TooltipButton;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.Drawing;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class DrawingColorCyclerButton extends TooltipButton {
    protected final Drawing.DrawingColorCycler drawingColorCycler;
    protected final IntSupplier alphaSupplier;
    public DrawingColorCyclerButton(final int x, final int y, final Supplier<Tooltip> tooltip, final Drawing.DrawingColorCycler drawingColorCycler) {
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
    public void renderButton(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
        int minX = this.x + 6;
        int minY = this.y + 6;
        if (this.isActive() && this.isHovered) {
            minY--;
        }
        int maxX = minX + 10;
        int maxY = minY + 10;
        fill(guiGraphics, minX, minY, maxX, maxY, drawingColorCycler.getColorInt(alphaSupplier.getAsInt()));
    }
}
