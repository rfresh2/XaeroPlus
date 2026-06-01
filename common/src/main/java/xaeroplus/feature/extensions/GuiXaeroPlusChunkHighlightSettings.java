package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.GuiSettings;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

public class GuiXaeroPlusChunkHighlightSettings extends GuiSettings {
    public GuiXaeroPlusChunkHighlightSettings(Screen parent, Screen escapeScreen) {
        super(Component.translatable("xaeroplus.gui.chunk_highlight_settings"), parent, escapeScreen);
        this.entries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.CHUNK_HIGHLIGHTS);
        this.canSkipWorldRender = true;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderEscapeScreen(guiGraphics, 0, 0, f);
        super.renderBackground(guiGraphics, i, j, f);
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry(Screen parent) {
        return new ScreenSwitchSettingEntry(
            "xaeroplus.gui.chunk_highlight_settings",
            GuiXaeroPlusChunkHighlightSettings::new,
            null,
            true
        );
    }
}
