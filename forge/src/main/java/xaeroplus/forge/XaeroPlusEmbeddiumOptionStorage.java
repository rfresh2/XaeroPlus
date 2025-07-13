package xaeroplus.forge;

import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import xaeroplus.settings.SettingHooks;

public class XaeroPlusEmbeddiumOptionStorage implements OptionStorage<Void> {
    public static final XaeroPlusEmbeddiumOptionStorage INSTANCE = new XaeroPlusEmbeddiumOptionStorage();

    @Override
    public Void getData() {
        return null;
    }

    @Override
    public void save() {
        SettingHooks.saveSettings();
    }
}
