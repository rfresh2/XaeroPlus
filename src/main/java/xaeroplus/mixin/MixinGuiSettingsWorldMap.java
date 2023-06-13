package xaeroplus.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.core.XaeroMinimapCore;
import xaero.common.gui.widget.WidgetScreenHandler;
import xaero.common.gui.widget.render.WidgetRenderer;
import xaero.map.gui.GuiSettings;
import xaero.map.gui.ScreenBase;

import static java.util.Objects.nonNull;

@Mixin(value = GuiSettings.class, remap = false)
public abstract class MixinGuiSettingsWorldMap extends ScreenBase {

    protected MixinGuiSettingsWorldMap(final Screen parent, final Screen escape, final Text titleIn) {
        super(parent, escape, titleIn);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = true)
    public void drawScreen(final DrawContext guiGraphics, final int mouseX, final int mouseY, final float delta, final CallbackInfo ci) {
        WidgetScreenHandler widgetScreenHandler = XaeroMinimapCore.modMain.getWidgetScreenHandler();
        ((MixinWidgetScreenHandlerAccessor) widgetScreenHandler).getWidgets().forEach(widget -> {
            WidgetRenderer widgetRenderer = widget.getType().widgetRenderer;
            if (nonNull(widgetRenderer)) {
                widgetRenderer.render(guiGraphics, width, height, mouseX, mouseY, delta, widget);
            }
        });
    }
}
