package xaeroplus.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiMinimapMain;
import xaero.common.gui.GuiMinimapSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.config.context.IEditConfigScreenContext;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

@Mixin(value = GuiMinimapMain.class, remap = false)
public abstract class MixinGuiMinimapMain extends GuiMinimapSettings {

    @Shadow
    private ISettingEntry[] mainEntries;

    public MixinGuiMinimapMain(final Component title, final Screen par1Screen, final Screen escScreen, final IEditConfigScreenContext context) {
        super(title, par1Screen, escScreen, context);
    }

    @Inject(method = "<init>(Lxaero/common/IXaeroMinimap;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/Screen;ZLxaero/lib/client/gui/config/context/IEditConfigScreenContext;)V",
        at = @At("RETURN"),
        remap = true) // $REMAP
    public void init(final CallbackInfo ci) {
        final XaeroPlusSettingEntry[] configSettingEntries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.MINIMAP_MAIN);
        final int oldLen = this.mainEntries.length;
        final int newLen = configSettingEntries.length;
        final int totalNewLen = oldLen + configSettingEntries.length;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        // todo: move below profile settings
        System.arraycopy(this.mainEntries, 0, newEntries, 0, 2);
        System.arraycopy(configSettingEntries, 0, newEntries, 2, newLen);
        System.arraycopy(this.mainEntries, 2, newEntries, 2 + newLen, oldLen - 2);
        this.mainEntries = newEntries;
    }
}

