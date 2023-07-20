package xaeroplus.settings;

import net.minecraft.client.resource.language.I18n;

public interface TranslatableSettingEnum {
    String getTranslationKey();

    default String getTranslatedName() {
        return I18n.translate(getTranslationKey());
    }
}
