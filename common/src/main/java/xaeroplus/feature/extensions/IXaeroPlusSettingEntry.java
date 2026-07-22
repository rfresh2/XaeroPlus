package xaeroplus.feature.extensions;

import xaero.lib.client.gui.ISettingEntry;
import xaeroplus.settings.XaeroPlusSetting;

public interface IXaeroPlusSettingEntry extends ISettingEntry {
    XaeroPlusSetting getXaeroPlusSetting();
}
