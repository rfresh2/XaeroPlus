package xaeroplus.fabric;

import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import xaeroplus.settings.SettingHooks;

public class XaeroPlusSodiumOptionStorage implements OptionStorage<Void> {
    public static final XaeroPlusSodiumOptionStorage INSTANCE = new XaeroPlusSodiumOptionStorage();

    @Override
    public Void getData() {
        return null;
    }

    @Override
    public void save() {
        SettingHooks.saveSettings();
    }
}
