package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

@Mixin(value = GuiWorldMapSettings.class, remap = false)
public abstract class MixinGuiWorldMapSettings extends GuiSettings {

    public MixinGuiWorldMapSettings(final ITextComponent title, final GuiScreen backScreen, final GuiScreen escScreen) {
        super(title, backScreen, escScreen);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/client/gui/GuiScreen;Lxaero/lib/client/gui/config/context/IEditConfigScreenContext;)V", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        final XaeroPlusSettingEntry[] configSettingEntries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.WORLD_MAP_MAIN);
        final int oldLen = this.entries.length;
        final int newLen = configSettingEntries.length;
        final int totalNewLen = oldLen + newLen;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        System.arraycopy(this.entries, 0, newEntries, 0, 2);
        System.arraycopy(configSettingEntries, 0, newEntries, 2, configSettingEntries.length);
        System.arraycopy(this.entries, 2, newEntries, 2 + configSettingEntries.length, oldLen - 2);
        this.entries = newEntries;
    }
}
