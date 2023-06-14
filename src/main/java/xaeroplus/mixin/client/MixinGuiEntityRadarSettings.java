package xaeroplus.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.AXaeroMinimap;
import xaero.common.gui.ConfigSettingEntry;
import xaero.common.gui.GuiEntityRadarSettings;
import xaero.common.gui.GuiMinimapSettings;
import xaero.common.gui.ISettingEntry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

@Mixin(value = GuiEntityRadarSettings.class, remap = false)
public abstract class MixinGuiEntityRadarSettings extends GuiMinimapSettings {

    public MixinGuiEntityRadarSettings(final AXaeroMinimap modMain, final Text title, final Screen par1Screen, final Screen escScreen) {
        super(modMain, title, par1Screen, escScreen);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final AXaeroMinimap modMain, final Screen backScreen, final Screen escScreen, final CallbackInfo ci) {
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
