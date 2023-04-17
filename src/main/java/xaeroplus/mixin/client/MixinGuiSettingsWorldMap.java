package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.core.XaeroMinimapCore;
import xaero.common.gui.widget.Widget;
import xaero.common.gui.widget.WidgetScreenHandler;
import xaero.common.gui.widget.render.WidgetRenderer;
import xaero.map.gui.GuiSettings;
import xaero.map.gui.ScreenBase;

import java.lang.reflect.Field;
import java.util.List;

import static java.util.Objects.nonNull;

@Mixin(value = GuiSettings.class, remap = false)
public abstract class MixinGuiSettingsWorldMap extends ScreenBase {

    protected MixinGuiSettingsWorldMap(final GuiScreen parent, final GuiScreen escape) {
        super(parent, escape);
    }

    @Inject(method = "drawScreen", at = @At("RETURN"), remap = true)
    public void drawScreen(final int par1, final int par2, final float par3, final CallbackInfo ci) {
        // old mixin ver bug prevents us from using accessor mixin here: https://github.com/SpongePowered/Mixin/issues/329
        try {
            WidgetScreenHandler widgetScreenHandler = XaeroMinimapCore.modMain.getWidgetScreenHandler();
            Field widgetsField = widgetScreenHandler.getClass().getDeclaredField("widgets");
            widgetsField.setAccessible(true);
            List<Widget> widgetsList = (List<Widget>) widgetsField.get(widgetScreenHandler);
            widgetsList.forEach(widget -> {
                WidgetRenderer widgetRenderer = widget.getType().widgetRenderer;
                if (nonNull(widgetRenderer)) {
                    widgetRenderer.render(width, height, par1, par2, 1f, widget);
                }
            });
        } catch (final Exception e) {
            // xd
        }
    }

    // todo: mixin on mouse over widget and click widget

}
