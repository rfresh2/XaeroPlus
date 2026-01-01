package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiMinimapSettings;
import xaero.common.gui.GuiWaypointSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.config.context.IEditConfigScreenContext;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

@Mixin(value = GuiWaypointSettings.class, remap = false)
public abstract class MixinGuiWaypointSettings extends GuiMinimapSettings {

    public MixinGuiWaypointSettings(final ITextComponent title, final GuiScreen par1Screen, final GuiScreen escScreen, final IEditConfigScreenContext context) {
        super(title, par1Screen, escScreen, context);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final CallbackInfo ci) {
        final XaeroPlusSettingEntry[] configSettingEntries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.WAYPOINTS);
        final int oldLen = this.entries.length;
        final int newLen = configSettingEntries.length;
        final int totalNewLen = oldLen + configSettingEntries.length;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        System.arraycopy(this.entries, 0, newEntries, newLen, oldLen);
        System.arraycopy(configSettingEntries, 0, newEntries, 0, newLen);
        this.entries = newEntries;
    }
}
