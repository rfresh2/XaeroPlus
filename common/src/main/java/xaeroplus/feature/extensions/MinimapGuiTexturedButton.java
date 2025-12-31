package xaeroplus.feature.extensions;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xaero.common.gui.TooltipButton;
import xaero.lib.client.gui.widget.Tooltip;

import java.util.function.Supplier;

public class MinimapGuiTexturedButton extends TooltipButton {
    public final int textureX;
    public final int textureY;
    public final int textureW;
    public final int textureH;
    public final ResourceLocation texture;

    public MinimapGuiTexturedButton(int x, int y, int w, int h, int textureX, int textureY, int textureW, int textureH, ResourceLocation texture, Button.OnPress onPress, Supplier<Tooltip> tooltip) {
        super(x, y, w, h, Component.empty(), onPress, tooltip);
        this.textureX = textureX;
        this.textureY = textureY;
        this.textureW = textureW;
        this.textureH = textureH;
        this.texture = texture;
    }

    @Override
    public Component getMessage() {
        return this.getXaero_tooltip() != null
            ? Component.literal((this.getXaero_tooltip().get()).getPlainText())
            : super.getMessage();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int iconX = this.getX() + this.width / 2 - this.textureW / 2;
        int iconY = this.getY() + this.height / 2 - this.textureH / 2;
        if (this.active) {
            if (this.isHovered) {
                --iconY;
                RenderSystem.setShaderColor(0.9F, 0.9F, 0.9F, 1.0F);
            } else {
                RenderSystem.setShaderColor(0.9882F, 0.9882F, 0.9882F, 1.0F);
            }
        } else {
            RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
        }
        guiGraphics.blit(this.texture, iconX, iconY, this.textureX, this.textureY, this.textureW, this.textureH);
    }
}
