package xaeroplus.util;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import xaero.map.gui.GuiSettings;
import xaero.map.gui.ISettingEntry;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

public class GuiXaeroPlusWorldMapSettings extends GuiSettings {

    public GuiXaeroPlusWorldMapSettings(Screen parent, Screen escapeScreen) {
        super(Text.translatable("gui.xaeroplus.world_map_settings"), parent, escapeScreen);
        this.entries = XaeroPlusSettingsReflectionHax.getWorldMapConfigSettingEntries().toArray(new ISettingEntry[0]);
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry(Screen parent) {
        return new ScreenSwitchSettingEntry(
            "gui.xaeroplus.world_map_settings",
            (current, escape) -> new GuiXaeroPlusWorldMapSettings(parent, escape),
            null,
            true
        );
    }
}
