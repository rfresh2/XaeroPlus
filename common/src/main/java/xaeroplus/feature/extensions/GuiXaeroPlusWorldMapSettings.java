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
    private final Screen parent;

    public GuiXaeroPlusWorldMapSettings(Screen parent, Screen escapeScreen) {
        super(Component.translatable("xaeroplus.gui.world_map_settings"), parent, escapeScreen);
        this.parent = parent;
        rebuildEntries();
        this.canSkipWorldRender = true;
    }

    @Override
    public void init() {
        rebuildEntries();
        super.init();
    }

    private void rebuildEntries() {
        var mainSettingsEntries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.WORLD_MAP_MAIN);
        var chunkHighlightSettingSwitchEntry = GuiXaeroPlusChunkHighlightSettings.getScreenSwitchSettingEntry(parent);
        var overlaySettingSwitchEntry = GuiXaeroPlusOverlaySettings.getScreenSwitchSettingEntry(parent);
        var drawOrderSettingSwitchEntry = DrawOrderScreen.getScreenSwitchSettingEntry();
        var customTeleportCommandEntry = GuiMinimapWaypointTeleportCommandSettings.getScreenSwitchSettingEntry();
        boolean showCustomTeleportCommand = Settings.REGISTRY.minimapWaypointTeleportMode.get() == Settings.MinimapWaypointTeleportMode.CUSTOM;
        int customEntryCount = showCustomTeleportCommand ? 1 : 0;
        this.entries = new ISettingEntry[mainSettingsEntries.length + 3 + customEntryCount];
        this.entries[0] = chunkHighlightSettingSwitchEntry;
        this.entries[1] = overlaySettingSwitchEntry;
        this.entries[2] = drawOrderSettingSwitchEntry;
        if (showCustomTeleportCommand) {
            this.entries[3] = customTeleportCommandEntry;
        }
        System.arraycopy(mainSettingsEntries, 0, this.entries, 3 + customEntryCount, mainSettingsEntries.length);
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
