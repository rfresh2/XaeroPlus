package xaeroplus.fabric;

import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;
import xaeroplus.settings.SettingHooks;

public class XaeroPlusSodium6OptionStorage implements OptionStorage<Void> {
    public static final XaeroPlusSodium6OptionStorage INSTANCE = new XaeroPlusSodium6OptionStorage();

    @Override
    public Void getData() {
        return null;
    }

    @Override
    public void save() {
        SettingHooks.saveSettings();
    }
}
