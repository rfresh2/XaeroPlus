package xaeroplus.event;

import net.minecraft.client.gui.GuiGraphics;

public class MinimapRenderEvent {
    public boolean cancelled = false;
    public Runnable postRenderCallback = null;
    public final GuiGraphics guiGraphics;
    public MinimapRenderEvent(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
    }
}
