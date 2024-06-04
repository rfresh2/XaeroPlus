package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.map.gui.GuiSettings;
import xaero.map.gui.ISettingEntry;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

public class GuiXaeroPlusChunkHighlightSettings extends GuiSettings {
    public GuiXaeroPlusChunkHighlightSettings(Screen parent, Screen escapeScreen) {
        super(Component.translatable("gui.xaeroplus.chunk_highlight_settings"), parent, escapeScreen);
        this.entries = XaeroPlusSettingsReflectionHax.getChunkHighlightConfigSettingEntries().toArray(new ISettingEntry[0]);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int par1, int par2, float par3) {
        this.renderEscapeScreen(guiGraphics, par1, par2, par3);
        super.render(guiGraphics, par1, par2, par3);
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry(Screen parent) {
        return new ScreenSwitchSettingEntry(
            "gui.xaeroplus.chunk_highlight_settings",
            GuiXaeroPlusChunkHighlightSettings::new,
            null,
            true
        );
    }
}
