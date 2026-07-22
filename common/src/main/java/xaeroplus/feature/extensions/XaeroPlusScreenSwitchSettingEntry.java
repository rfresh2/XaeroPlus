package xaeroplus.feature.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import xaero.common.gui.TooltipButton;
import xaero.lib.client.gui.ScreenBase;
import xaero.lib.client.gui.widget.Tooltip;
import xaeroplus.settings.StringSetting;
import xaeroplus.settings.XaeroPlusSetting;

public class XaeroPlusScreenSwitchSettingEntry implements IXaeroPlusSettingEntry {
    private final StringSetting xaeroPlusSetting;

    public XaeroPlusScreenSwitchSettingEntry(final StringSetting xaeroPlusSetting) {
        this.xaeroPlusSetting = xaeroPlusSetting;
    }

    @Override
    public XaeroPlusSetting getXaeroPlusSetting() {
        return xaeroPlusSetting;
    }

    @Override
    public String getStringForSearch() {
        return xaeroPlusSetting.getTranslatedName();
    }

    @Override
    public AbstractWidget createWidget(final int x, final int y, final int w) {
        var button = new TooltipButton(x, y, w, 20, Component.translatable(xaeroPlusSetting.getSettingNameTranslationKey()), (b) -> {
            var mc = Minecraft.getInstance();
            var current = mc.gui.screen();
            var currentEscScreen = current instanceof ScreenBase ? ((ScreenBase) current).escape : null;
            var targetScreen = xaeroPlusSetting.getScreenSupplier().getScreen(current, currentEscScreen, xaeroPlusSetting);
            mc.gui.setScreen(targetScreen);
        }, () -> new Tooltip(xaeroPlusSetting.getTooltipTranslationKey()));
        button.active = xaeroPlusSetting.isVisible();
        return button;
    }
}
