package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiMinimapSettings;
import xaero.common.gui.GuiWaypointSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.config.context.IEditConfigScreenContext;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.feature.extensions.GuiMinimapWaypointTeleportCommandSettings;
import xaeroplus.feature.extensions.WaypointSettingsEntryRefresher;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

@Mixin(value = GuiWaypointSettings.class, remap = false)
public class MixinGuiWaypointSettings extends GuiMinimapSettings implements WaypointSettingsEntryRefresher {
    @Unique
    private ISettingEntry[] xaeroplus$baseEntries;

    public MixinGuiWaypointSettings(final Component title, final Screen par1Screen, final Screen escScreen, final IEditConfigScreenContext context) {
        super(title, par1Screen, escScreen, context);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final CallbackInfo ci) {
        this.xaeroplus$baseEntries = this.entries;
        xaeroplus$rebuildEntries();
    }

    @Override
    public void xaeroplus$refreshEntries() {
        xaeroplus$rebuildEntries();
        this.resize(
            Minecraft.getInstance(),
            Minecraft.getInstance().getWindow().getGuiScaledWidth(),
            Minecraft.getInstance().getWindow().getGuiScaledHeight()
        );
    }

    @Unique
    private void xaeroplus$rebuildEntries() {
        final XaeroPlusSettingEntry[] configSettingEntries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.MINIMAP_WAYPOINTS);
        final ScreenSwitchSettingEntry[] formatSettingEntries = Settings.REGISTRY.useCustomCrossDimensionWaypointTeleportFormat.get()
            ? GuiMinimapWaypointTeleportCommandSettings.getScreenSwitchSettingEntries()
            : new ScreenSwitchSettingEntry[0];
        final int oldLen = this.xaeroplus$baseEntries.length;
        final int newLen = configSettingEntries.length;
        final int formatLen = formatSettingEntries.length;
        final int totalNewLen = oldLen + newLen + formatLen;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        System.arraycopy(configSettingEntries, 0, newEntries, 0, newLen);
        System.arraycopy(formatSettingEntries, 0, newEntries, newLen, formatLen);
        System.arraycopy(this.xaeroplus$baseEntries, 0, newEntries, newLen + formatLen, oldLen);
        this.entries = newEntries;
    }
}
