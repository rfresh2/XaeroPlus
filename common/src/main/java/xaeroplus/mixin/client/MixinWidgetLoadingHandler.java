package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.gui.GuiSettings;
import xaero.common.gui.widget.*;

@Mixin(value = WidgetLoadingHandler.class, remap = false)
public class MixinWidgetLoadingHandler {
    @Shadow
    private WidgetScreenHandler handler;

    /**
     * @author rfresh2
     * @reason insert XaeroPlus widget
     */
    @Overwrite
    public void loadWidget(String serialized) {
        ((AccessorWidgetScreenHandler) handler).getWidgets().clear();
        final TextWidgetBuilder builder = new TextWidgetBuilder();
        builder.setText("§7§kaa§r §l§bXaero§aPlus§r §7§kaa§r");
        builder.setAlignment(Alignment.CENTER);
        builder.setOnClick(ClickAction.URL);
        builder.setUrl("https://github.com/rfresh2/XaeroPlus");
        builder.setTooltip("Click to open the link");
        builder.setOnHover(HoverAction.TOOLTIP);
        builder.setX(0);
        builder.setY(20);
        builder.setHorizontalAnchor(0.5f);
        builder.setVerticalAnchor(0.0f);
        builder.setNoGuiScale(false);
        builder.setLocation(GuiSettings.class);
        Widget widget = builder.build();
        ((AccessorWidgetScreenHandler) handler).invokeAddWidget(widget);
    }
}
