package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.ConfigSettingEntry;
import xaero.common.gui.GuiEntityRadarSettings;
import xaero.common.gui.GuiMinimapSettings;
import xaero.common.gui.ISettingEntry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

@Mixin(value = GuiEntityRadarSettings.class, remap = false)
public abstract class MixinGuiEntityRadarSettings extends GuiMinimapSettings {
    public MixinGuiEntityRadarSettings(final IXaeroMinimap modMain, final ITextComponent title, final GuiScreen backScreen, final GuiScreen escScreen) {
        super(modMain, title, backScreen, escScreen);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final IXaeroMinimap modMain, final GuiScreen parent, final GuiScreen escape, final CallbackInfo ci) {
        final ConfigSettingEntry[] configSettingEntries = XaeroPlusSettingsReflectionHax.getMiniMapEntityRadarSettingEntries()
                .toArray(new ConfigSettingEntry[0]);
        final int oldLen = this.entries.length;
        final int newLen = configSettingEntries.length;
        final int totalNewLen = oldLen + configSettingEntries.length;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        System.arraycopy(this.entries, 0, newEntries, newLen, oldLen);
        System.arraycopy(configSettingEntries, 0, newEntries, 0, newLen);
        this.entries = newEntries;
    }
}
