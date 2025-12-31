package xaeroplus.feature.extensions;

import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.CustomSettingEntry;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaeroplus.settings.XaeroPlusSetting;

import java.util.function.*;

public class XaeroPlusSettingEntry<T> extends CustomSettingEntry<T> {
    private final XaeroPlusSetting xaeroPlusSetting;
    public XaeroPlusSettingEntry(
        final XaeroPlusSetting xaeroPlusSetting,
        final Component name,
        final TooltipInfo tooltipInfo,
        final boolean slider,
        final Supplier<T> currentValueSupplier,
        final int minIndex,
        final int maxIndex,
        final IntFunction<T> indexReader,
        final Function<T, Component> valueNamer,
        final BiConsumer<T, T> onValueChange,
        final BooleanSupplier activeSupplier
    ) {
        super(
            () -> false,
            name,
            tooltipInfo,
            slider,
            currentValueSupplier,
            minIndex,
            maxIndex,
            indexReader,
            valueNamer,
            onValueChange,
            activeSupplier);
        this.xaeroPlusSetting = xaeroPlusSetting;
    }

    public XaeroPlusSetting getXaeroPlusSetting() {
        return xaeroPlusSetting;
    }
}
