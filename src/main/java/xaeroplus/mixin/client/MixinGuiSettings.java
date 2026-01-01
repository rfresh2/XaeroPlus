package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.common.util.KeySortableByOther;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;

import java.util.ArrayList;

@Mixin(value = GuiSettings.class, remap = false)
public class MixinGuiSettings {
    @Redirect(method = "initGui", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"), remap = true)
    public boolean settingListToRenderRedirect(final ArrayList instance, final Object entryObject) {
        final KeySortableByOther<ISettingEntry> entry = (KeySortableByOther<ISettingEntry>) entryObject;
        ISettingEntry settingEntry = entry.getKey();
        if (settingEntry instanceof XaeroPlusSettingEntry<?>) {
            if (!((XaeroPlusSettingEntry<?>) settingEntry).getXaeroPlusSetting().isVisible()) {
                return false;
            }
        }
        instance.add(entryObject);
        return false;
    }
}
