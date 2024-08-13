package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import xaero.common.gui.widget.Widget;
import xaero.common.gui.widget.WidgetScreenHandler;

import java.util.List;

@Mixin(value = WidgetScreenHandler.class, remap = false)
public interface AccessorWidgetScreenHandler {

    @Invoker(value = "addWidget")
    void invokeAddWidget(Widget widget);

    @Accessor(value = "widgets")
    List<Widget> getWidgets();
}
