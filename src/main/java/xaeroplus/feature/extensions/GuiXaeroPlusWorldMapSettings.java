package xaeroplus.feature.extensions;

import net.minecraft.client.gui.DrawContext;
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

    @Override
    public void render(DrawContext guiGraphics, int par1, int par2, float par3) {
        this.renderEscapeScreen(guiGraphics, par1, par2, par3);
        super.render(guiGraphics, par1, par2, par3);
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry(Screen parent) {
        return new ScreenSwitchSettingEntry(
            "gui.xaeroplus.world_map_settings",
            GuiXaeroPlusWorldMapSettings::new,
            null,
            true
        );
    }
}
