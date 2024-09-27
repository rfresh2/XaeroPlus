package xaeroplus.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.ConfigSettingEntry;
import xaero.common.gui.GuiMinimapViewSettings;
import xaero.common.gui.GuiSettings;
import xaero.common.gui.ISettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

@Mixin(value = GuiMinimapViewSettings.class, remap = false)
public abstract class MixinMinimapViewSettings extends GuiSettings {

    public MixinMinimapViewSettings(final IXaeroMinimap modMain, final Component title, final Screen backScreen, final Screen escScreen) {
        super(modMain, title, backScreen, escScreen);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final IXaeroMinimap modMain, final Screen backScreen, final Screen escScreen, final CallbackInfo ci) {
        final ConfigSettingEntry[] configSettingEntries = Settings.REGISTRY.getMinimapConfigSettingEntries(SettingLocation.MINIMAP_VIEW);
        final int oldLen = this.entries.length;
        final int newLen = configSettingEntries.length;
        final int totalNewLen = oldLen + configSettingEntries.length;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        System.arraycopy(this.entries, 0, newEntries, newLen, oldLen);
        System.arraycopy(configSettingEntries, 0, newEntries, 0, newLen);
        this.entries = newEntries;
    }
}
