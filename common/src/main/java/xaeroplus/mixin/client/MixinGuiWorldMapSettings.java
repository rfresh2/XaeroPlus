package xaeroplus.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.config.EditConfigScreen;
import xaero.lib.client.gui.config.context.IEditConfigScreenContext;
import xaero.lib.common.config.channel.ConfigChannel;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.feature.extensions.GuiXaeroPlusWorldMapSettings;

@Mixin(value = GuiWorldMapSettings.class, remap = false)
public abstract class MixinGuiWorldMapSettings extends EditConfigScreen {
    public MixinGuiWorldMapSettings(final Component title, final Screen backScreen, final Screen escScreen, final IEditConfigScreenContext context, final ConfigChannel channel) {
        super(title, backScreen, escScreen, context, channel);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/Screen;Lxaero/lib/client/gui/config/context/IEditConfigScreenContext;)V", at = @At("RETURN"), remap = true)
    public void init(final Screen parent, final Screen escapeScreen, final IEditConfigScreenContext context, final CallbackInfo ci) {
        final int oldLen = this.entries.length;
        final int totalNewLen = oldLen + 1;
        final ISettingEntry[] newEntries = new ISettingEntry[totalNewLen];
        var xpScreenSwitchEntry = GuiXaeroPlusWorldMapSettings.getScreenSwitchSettingEntry(parent);
        System.arraycopy(this.entries, 0, newEntries, 0, 2);
        newEntries[2] = xpScreenSwitchEntry;
        System.arraycopy(this.entries, 2, newEntries, 3, oldLen - 2);
        this.entries = newEntries;
    }
}
