package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.gui.ConfigSettingEntry;
import xaero.map.gui.GuiSettings;
import xaero.map.gui.ISettingEntry;
import xaero.map.misc.KeySortableByOther;
import xaero.map.settings.ModOptions;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSetting;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;

@Mixin(value = GuiSettings.class, remap = false)
public class MixinWorldMapGuiSettings {
    @Redirect(method = "initGui", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"), remap = true)
    public boolean settingListToRenderRedirect(final ArrayList instance, final Object entryObject) {
        final KeySortableByOther<ISettingEntry> entry = (KeySortableByOther<ISettingEntry>) entryObject;
        ISettingEntry settingEntry = entry.getKey();
        if (settingEntry instanceof ConfigSettingEntry) {
            try {
                Field option = ConfigSettingEntry.class.getDeclaredField("option");
                option.setAccessible(true);
                ModOptions modOptions = (ModOptions) option.get(settingEntry);
                String settingName = modOptions.getEnumString();
                Optional<XaeroPlusSetting> foundSetting = XaeroPlusSettingsReflectionHax.XAERO_PLUS_WORLDMAP_SETTINGS.stream()
                        .filter(s -> s.getSettingName().equals(settingName))
                        .findFirst();
                if (foundSetting.isPresent()) {
                    if (!foundSetting.get().isVisible()) {
                        // skip adding setting
                        return false;
                    }
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.warn("Failed to edit settings gui", e);
            }
        }
        instance.add(entryObject);
        return false;
    }
}
