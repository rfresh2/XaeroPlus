package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

public class GuiXaeroPlusWorldMapSettings extends GuiSettings {

    public GuiXaeroPlusWorldMapSettings(Screen parent, Screen escapeScreen) {
        super(Component.translatable("xaeroplus.gui.world_map_settings"), parent, escapeScreen);
        var mainSettingsEntries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.WORLD_MAP_MAIN);
        var chunkHighlightSettingSwitchEntry = GuiXaeroPlusChunkHighlightSettings.getScreenSwitchSettingEntry(parent);
        var overlaySettingSwitchEntry = GuiXaeroPlusOverlaySettings.getScreenSwitchSettingEntry(parent);
        var drawOrderSettingSwitchEntry = DrawOrderScreen.getScreenSwitchSettingEntry();
        this.entries = new ISettingEntry[mainSettingsEntries.length + 3];
        this.entries[0] = chunkHighlightSettingSwitchEntry;
        this.entries[1] = overlaySettingSwitchEntry;
        this.entries[2] = drawOrderSettingSwitchEntry;
        System.arraycopy(mainSettingsEntries, 0, this.entries, 3, mainSettingsEntries.length);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int par1, int par2, float par3) {
        this.renderEscapeScreen(guiGraphics, par1, par2, par3);
        super.render(guiGraphics, par1, par2, par3);
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry(Screen parent) {
        return new ScreenSwitchSettingEntry(
            "xaeroplus.gui.world_map_settings",
            GuiXaeroPlusWorldMapSettings::new,
            null,
            true
        );
    }
}
