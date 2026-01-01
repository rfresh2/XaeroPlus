package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiMinimapMain;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;
import xaeroplus.settings.SettingLocation;
import xaeroplus.settings.Settings;

@Mixin(value = GuiMinimapMain.class, remap = false)
public abstract class MixinGuiMinimapMain extends GuiSettings {

    @Shadow private ISettingEntry[] mainEntries;

    public MixinGuiMinimapMain(final ITextComponent title, final GuiScreen backScreen, final GuiScreen escScreen) {
        super(title, backScreen, escScreen);
    }

    @Inject(method = "<init>(Lxaero/common/IXaeroMinimap;Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/client/gui/GuiScreen;ZLxaero/lib/client/gui/config/context/IEditConfigScreenContext;)V", at = @At("RETURN"))
    public void init(final CallbackInfo ci) {
        final XaeroPlusSettingEntry[] configSettingEntries = Settings.REGISTRY.getXaeroSettingEntries(SettingLocation.MINIMAP);
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
