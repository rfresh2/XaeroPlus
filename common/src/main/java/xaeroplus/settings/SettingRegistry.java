package xaeroplus.settings;

import net.minecraft.client.KeyMapping;
import xaero.lib.client.gui.CustomSettingEntry;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SettingRegistry {
    private final Map<SettingLocation, ArrayList<XaeroPlusSetting>> settingLocationMap = new EnumMap<>(SettingLocation.class);
    private final Map<String, XaeroPlusSetting> settingNameMap = new ConcurrentHashMap<>();
    private final Map<KeyMapping, BooleanSetting> keybindingMap = new ConcurrentHashMap<>();

    public BooleanSetting register(BooleanSetting setting, SettingLocation settingLocation) {
        register0(settingLocation, setting);
        return setting;
    }

    public DoubleSetting register(DoubleSetting setting, SettingLocation settingLocation) {
        register0(settingLocation, setting);
        return setting;
    }

    public <E extends Enum<E>> EnumSetting<E> register(EnumSetting<E> setting, SettingLocation settingLocation) {
        register0(settingLocation, setting);
        return setting;
    }

    public StringSetting register(StringSetting setting, SettingLocation settingLocation) {
        register0(settingLocation, setting);
        return setting;
    }

    private synchronized void register0(SettingLocation settingLocation, XaeroPlusSetting setting) {
        if (settingNameMap.containsKey(setting.getSettingName())) {
            throw new RuntimeException("Setting with name '" + setting.getSettingName() + "' already exists");
        }
        var settingList = this.settingLocationMap.getOrDefault(settingLocation, new ArrayList<>());
        settingList.add(setting);
        this.settingLocationMap.put(settingLocation, settingList);
        this.settingNameMap.put(setting.getSettingName(), setting);
        if (setting instanceof BooleanSetting booleanSetting) {
            var kb = booleanSetting.getKeyBinding();
            if (kb != null) keybindingMap.put(kb, booleanSetting);
        }
    }

    public XaeroPlusSetting getSettingByName(String name) {
        return settingNameMap.get(name);
    }

    public Set<KeyMapping> getKeybindings() {
        return keybindingMap.keySet();
    }

    public BooleanSetting getKeybindingSetting(KeyMapping keyMapping) {
        return keybindingMap.get(keyMapping);
    }

    public List<XaeroPlusSetting> getAllSettings() {
        return new ArrayList<>(settingNameMap.values());
    }

    public synchronized XaeroPlusSettingEntry[] getXaeroSettingEntries(SettingLocation settingLocation) {
        var settingList = this.settingLocationMap.get(settingLocation);
        if (settingList != null) {
            List<CustomSettingEntry<?>> entries = new ArrayList<>(settingList.size());
            for (int i = 0; i < settingList.size(); i++) {
                final XaeroPlusSetting xaeroPlusSetting = settingList.get(i);
                var entry = xaeroPlusSetting.toXaeroSettingEntry();
                if (entry != null) entries.add(entry);
            }
            return entries.toArray(new XaeroPlusSettingEntry[0]);
        }
        return new XaeroPlusSettingEntry[0];
    }
}
