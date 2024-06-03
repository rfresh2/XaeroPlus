package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.ConfigSettingEntry;
import xaero.common.gui.GuiMinimapMain;
import xaero.common.gui.GuiSettings;
import xaero.common.gui.ISettingEntry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

@Mixin(value = GuiMinimapMain.class, remap = false)
public abstract class MixinGuiMinimapMain extends GuiSettings {

    @Shadow private ISettingEntry[] mainEntries;

    public MixinGuiMinimapMain(final IXaeroMinimap modMain, final ITextComponent title, final GuiScreen backScreen, final GuiScreen escScreen) {
        super(modMain, title, backScreen, escScreen);
    }

    @Inject(method = "<init>(Lxaero/common/IXaeroMinimap;Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/client/gui/GuiScreen;)V", at = @At("RETURN"))
    public void init(final IXaeroMinimap modMain, final GuiScreen backScreen, final GuiScreen escScreen, final CallbackInfo ci) {
        final ConfigSettingEntry[] configSettingEntries = XaeroPlusSettingsReflectionHax.getMiniMapConfigSettingEntries()
                .toArray(new ConfigSettingEntry[0]);
        final int oldLen = this.mainEntries.length;
        final int newLen = configSettingEntries.length;
        final int totalNewLen = oldLen + configSettingEntries.length;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        System.arraycopy(this.mainEntries, 0, newEntries, newLen, oldLen);
        System.arraycopy(configSettingEntries, 0, newEntries, 0, newLen);
        this.mainEntries = newEntries;
    }
}
