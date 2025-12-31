package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.GuiSettings;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

public class GuiXaeroPlusOverlaySettings extends GuiSettings {
    public GuiXaeroPlusOverlaySettings(Screen parent, Screen escapeScreen) {
        super(Component.translatable("xaeroplus.gui.overlay_settings"), parent, escapeScreen);
        this.entries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.OVERLAYS);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderEscapeScreen(guiGraphics, 0, 0, f);
        super.renderBackground(guiGraphics, i, j, f);
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry(Screen parent) {
        return new ScreenSwitchSettingEntry(
            "xaeroplus.gui.overlay_settings",
            GuiXaeroPlusOverlaySettings::new,
            null,
            true
        );
    }
}
