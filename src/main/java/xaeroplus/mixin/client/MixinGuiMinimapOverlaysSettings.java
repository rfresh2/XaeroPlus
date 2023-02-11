package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.*;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = GuiMinimapOverlaysSettings.class, remap = false)
public abstract class MixinGuiMinimapOverlaysSettings extends GuiSettings {

    public MixinGuiMinimapOverlaysSettings(IXaeroMinimap modMain, String title, GuiScreen backScreen, GuiScreen escScreen) {
        super(modMain, title, backScreen, escScreen);
    }

    @Inject(method = "<init>(Lxaero/common/IXaeroMinimap;Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/client/gui/GuiScreen;)V", at = @At("RETURN"))
    public void init(final IXaeroMinimap modMain, GuiScreen backScreen, GuiScreen escScreen, CallbackInfo ci) {
        final ConfigSettingEntry[] configSettingEntries = XaeroPlusSettingRegistry.getMiniMapConfigSettingEntries()
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
