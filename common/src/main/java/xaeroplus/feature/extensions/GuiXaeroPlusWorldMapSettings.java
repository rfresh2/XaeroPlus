package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.ScreenBase;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

public class GuiXaeroPlusWorldMapSettings extends GuiSettings {

    public GuiXaeroPlusWorldMapSettings(Screen parent, Screen escapeScreen) {
        super(Component.translatable("xaeroplus.gui.world_map_settings"), parent, escapeScreen);
        var mainSettingsEntries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.WORLD_MAP_MAIN);
        var chunkHighlightSettingSwitchEntry = GuiXaeroPlusChunkHighlightSettings.getScreenSwitchSettingEntry(parent);
        var overlaySettingSwitchEntry = GuiXaeroPlusOverlaySettings.getScreenSwitchSettingEntry(parent);
        this.entries = new ISettingEntry[mainSettingsEntries.length + 2];
        this.entries[0] = chunkHighlightSettingSwitchEntry;
        this.entries[1] = overlaySettingSwitchEntry;
        System.arraycopy(mainSettingsEntries, 0, this.entries, 2, mainSettingsEntries.length);
        this.canSkipWorldRender = true;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        if (this.escape instanceof ScreenBase) {
            this.renderEscapeScreen(guiGraphics, 0, 0, f);
        }
        super.renderBackground(guiGraphics, i, j, f);
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
