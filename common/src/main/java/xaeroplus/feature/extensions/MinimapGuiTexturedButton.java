package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xaero.common.graphics.CursorBox;
import xaero.common.gui.TooltipButton;
import xaeroplus.util.ColorHelper;

import java.util.function.Supplier;

public class MinimapGuiTexturedButton extends TooltipButton {
    public final int textureX;
    public final int textureY;
    public final int textureW;
    public final int textureH;
    protected final int factorW;
    protected final int factorH;
    public final Identifier texture;

    public MinimapGuiTexturedButton(int x, int y, int w, int h, int textureX, int textureY, int textureW, int textureH, Identifier texture, Button.OnPress onPress, Supplier<CursorBox> tooltip, int factorW, int factorH) {
        super(x, y, w, h, Component.empty(), onPress, tooltip);
        this.textureX = textureX;
        this.textureY = textureY;
        this.textureW = textureW;
        this.textureH = textureH;
        this.texture = texture;
        this.factorW = factorW;
        this.factorH = factorH;
    }

    @Override
    public Component getMessage() {
        return this.getXaero_tooltip() != null
            ? Component.literal((this.getXaero_tooltip().get()).getPlainText())
            : super.getMessage();
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int iconX = this.getX() + this.width / 2 - this.textureW / 2;
        int iconY = this.getY() + this.height / 2 - this.textureH / 2;
        int color = ColorHelper.getColor(64, 64, 64, 255);
        if (this.active) {
            if (this.isHovered) {
                --iconY;
                color = ColorHelper.getColor(230, 230, 230, 255);
            } else {
                color = ColorHelper.getColor(252, 252, 252, 255);
            }
        }

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, iconX, iconY, (float)this.textureX, (float)this.textureY, this.textureW, this.textureH, this.factorW, this.factorH, color);
    }
}
