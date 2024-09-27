package xaeroplus.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiSettings;
import xaero.map.gui.GuiWorldMapSettings;
import xaero.map.gui.ISettingEntry;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.feature.extensions.GuiXaeroPlusWorldMapSettings;

@Mixin(value = GuiWorldMapSettings.class, remap = false)
public abstract class MixinGuiWorldMapSettings extends GuiSettings {
    public MixinGuiWorldMapSettings(final Component title, final Screen backScreen, final Screen escScreen) {
        super(title, backScreen, escScreen);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("RETURN"), remap = true)
    public void init(final Screen parent, final Screen escapeScreen, final CallbackInfo ci) {
        final int oldLen = this.entries.length;
        final int newLen = 1;
        final int totalNewLen = oldLen + 1;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        newEntries[0] = GuiXaeroPlusWorldMapSettings.getScreenSwitchSettingEntry(parent);
        System.arraycopy(this.entries, 0, newEntries, newLen, oldLen);
        this.entries = newEntries;

        // swap minimap settings onto first page
        int minimapSettingEntryIndex = -1;
        for (int i = 0; i < entries.length; i++) {
            var entry = entries[i];
            if (entry instanceof ScreenSwitchSettingEntry screenSwitchEntry) {
                var name = ((AccessorWorldMapScreenSwitchSettingEntry) screenSwitchEntry).getName();
                if (name.equals("xaeroplus.gui.xaero_wm_minimap_settings")) {
                    minimapSettingEntryIndex = i;
                    break;
                }
            }
        }
        if (minimapSettingEntryIndex > 1) {
            var minimapEntry = this.entries[minimapSettingEntryIndex];
            this.entries[minimapSettingEntryIndex] = this.entries[minimapSettingEntryIndex-1];
            this.entries[minimapSettingEntryIndex-1] = minimapEntry;
        }
    }
}
