package xaeroplus.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.ConfigSettingEntry;
import xaero.map.gui.GuiSettings;
import xaero.map.gui.GuiWorldMapSettings;
import xaero.map.gui.ISettingEntry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

@Mixin(value = GuiWorldMapSettings.class, remap = false)
public abstract class MixinGuiWorldMapSettings extends GuiSettings {
    public MixinGuiWorldMapSettings(final Text title, final Screen backScreen, final Screen escScreen) {
        super(title, backScreen, escScreen);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("RETURN"))
    public void init(final Screen parent, final Screen escapeScreen, final CallbackInfo ci) {
        final ConfigSettingEntry[] configSettingEntries = XaeroPlusSettingsReflectionHax.getWorldMapConfigSettingEntries()
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
